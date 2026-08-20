package com.cms.service.impl;

import com.cms.dal.entity.Card;
import com.cms.dal.entity.CardAccount;
import com.cms.dal.entity.CardRequest;
import com.cms.dal.entity.LimitProfile;
import com.cms.dal.repository.AccountRepository;
import com.cms.dal.repository.CardAccountRepository;
import com.cms.dal.repository.CardRepository;
import com.cms.dal.repository.CardRequestRepository;
import com.cms.dal.repository.CardTypeRepository;
import com.cms.dal.repository.LimitProfileRepository;
import com.cms.dto.response.CardGenerationResultResponse;
import com.cms.dto.response.CardRequestResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.mapper.CardMapper;
import com.cms.mapper.CardRequestMapper;
import com.cms.service.CardDataEncryptionService;
import com.cms.service.CardGenerationService;
import com.cms.service.CardTrackDataFormatter;
import com.cms.service.CvvGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.time.YearMonth;
import java.util.Random; // Modified this code for PAN generation
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;


@Service
public class CardGenerationServiceImpl implements CardGenerationService {

    private static final Logger log = LoggerFactory.getLogger(CardGenerationServiceImpl.class);

    /** Mobile app new-card request types (matches LOV NEW / 1 and explicit MOBILE). */
    private static final Set<String> MOBILE_REQUEST_TYPES = Set.of("NEW", "1", "MOBILE");

    private final CardRequestRepository cardRequestRepository;
    private final CardRepository cardRepository;
    private final CardTypeRepository cardTypeRepository;
    private final LimitProfileRepository limitProfileRepository;
    private final CardRequestMapper cardRequestMapper;
    private final CardMapper cardMapper;
    private final CardDataEncryptionService encryptionService;
    private final CvvGenerationService cvvGenerationService;
    private final CardTrackDataFormatter cardTrackDataFormatter;
    private final AccountRepository accountRepository;
    private final CardAccountRepository cardAccountRepository;
    private final String mobileDefaultLimitProfile;

    private static final DateTimeFormatter EXPIRY_YYMM = DateTimeFormatter.ofPattern("yyMM");

    public CardGenerationServiceImpl(CardRequestRepository cardRequestRepository, CardRepository cardRepository,
                                    CardTypeRepository cardTypeRepository,
                                    LimitProfileRepository limitProfileRepository,
                                    CardRequestMapper cardRequestMapper,
                                    CardMapper cardMapper, CardDataEncryptionService encryptionService,
                                    CvvGenerationService cvvGenerationService,
                                    CardTrackDataFormatter cardTrackDataFormatter,
                                    AccountRepository accountRepository,
                                    CardAccountRepository cardAccountRepository,
                                    @Value("${cms.card.mobile-default-limit-profile:STD}") String mobileDefaultLimitProfile) {
        this.cardRequestRepository = cardRequestRepository;
        this.cardRepository = cardRepository;
        this.cardTypeRepository = cardTypeRepository;
        this.limitProfileRepository = limitProfileRepository;
        this.cardRequestMapper = cardRequestMapper;
        this.cardMapper = cardMapper;
        this.encryptionService = encryptionService;
        this.cvvGenerationService = cvvGenerationService;
        this.cardTrackDataFormatter = cardTrackDataFormatter;
        this.accountRepository = accountRepository;
        this.cardAccountRepository = cardAccountRepository;
        this.mobileDefaultLimitProfile = mobileDefaultLimitProfile;
    }

    @Override
    public List<CardRequestResponse> getCardRequestByCode(String relationshipNum, String accountNum) {
        List<CardRequest> list = cardRequestRepository.findByRelationshipNumAndAccountNum(relationshipNum, accountNum);
        return cardRequestMapper.toResponseList(list);
    }

    @Override
    @Transactional
    public CardGenerationResultResponse processNewCardGeneration(Long requestId) {
        CardGenerationResultResponse result = new CardGenerationResultResponse();
        CardRequest req = cardRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("CardRequest", String.valueOf(requestId)));
        if (req.getIsProcessed() != null && req.getIsProcessed() == 1) {
            result.setSuccess(false);
            result.setMessage("Request already processed");
            return result;
        }
        boolean isReplacement = "REPLACEMENT".equalsIgnoreCase(req.getRequestTypeId());
        Optional<Card> replacementTarget = isReplacement ? findReplacementTargetCard(req) : Optional.empty();
        Card card = replacementTarget.orElseGet(Card::new);
        String oldPan = resolvePan(card);
        card.setRelationshipNum(req.getRelationshipNum());
        // Modified code for setting title to 19 chars
        String rawTitle = req.getCardTitle() != null ? req.getCardTitle().trim() : "";
        card.setCardTitle(String.format("%-19s", rawTitle));
        card.setCardTypeCode(req.getCardTypeCode());
        card.setProductCode(req.getProductCode());
        card.setBranchCode(req.getBranchCode());
        if (req.getRequestTypeId() != null && !req.getRequestTypeId().isBlank()) {
            card.setRequestType(req.getRequestTypeId());
        }

        // Modified this code for PAN generation — use cardTypeCode since cardTypeId removed from CardRequest
        String generatedPan = generatePan(null, req.getCardTypeCode());
        String panMasked = encryptionService.maskPan(generatedPan);
        // Store masked PAN only in clear columns; full PAN lives in PAN_ENCRYPTED
        card.setPan(panMasked);
        card.setPanEncrypted(encryptionService.encrypt(generatedPan));
        card.setPanLast4(encryptionService.panLast4(generatedPan));
        card.setPanHash(encryptionService.panHashForLookup(generatedPan));
        card.setTrimPan(panMasked);
        YearMonth expiryMonth = YearMonth.now().plusYears(5);
        card.setExpiryDate(expiryMonth.atEndOfMonth().atTime(23, 59, 59));
        //card.setExpiryDate(LocalDateTime.now().plusYears(5));
        card.setCardStatusCode("001");
        card.setCreatedOn(LocalDateTime.now());
        card.setUpdatedOn(LocalDateTime.now());
        card.setCreatedBy("system");
        card.setUpdatedBy("system");
        card.setIsReplaced(0);
        card.setIssuedDate(LocalDateTime.now());
        card.setActivationDate(null);
        card.setCardProdStatusId("001"); // 001 = Issued, card generated not yet exported

        // Mobile app requests approved on portal: auto-assign standard limit profile
        applyMobileStandardLimitIfNeeded(req, card);

        String expiryYyMm = card.getExpiryDate().format(EXPIRY_YYMM);
        CvvGenerationService.CvvResult cvvResult = cvvGenerationService.generate(generatedPan, expiryYyMm);
        String track1 = cardTrackDataFormatter.formatTrack1(generatedPan, expiryYyMm, card.getCardTitle(), cvvResult.cvv1());
        String track2 = cardTrackDataFormatter.formatTrack2(generatedPan, expiryYyMm, cvvResult.cvv1());

        card.setCvv(encryptionService.encrypt(cvvResult.cvv1()));
        card.setCvv2(encryptionService.encrypt(cvvResult.cvv2()));
        card.setIcvv(encryptionService.encrypt(cvvResult.icvv()));
        card.setTrack1Data(encryptionService.encrypt(track1));
        card.setTrack2Data(encryptionService.encrypt(track2));

        Card saved = cardRepository.save(card);
        if (isReplacement && oldPan != null && !oldPan.isBlank() && !oldPan.equals(generatedPan)) {
            // Keep account links on the same card row aligned with the new masked PAN after replacement.
            List<CardAccount> links = cardAccountRepository.findByCardId(saved.getCardId());
            for (CardAccount link : links) {
                link.setPan(panMasked);
                link.setUpdatedOn(LocalDateTime.now());
                link.setUpdatedBy("system");
            }
            cardAccountRepository.saveAll(links);
        }
        req.setIsProcessed(1);
        req.setProgressFlag(1);
        req.setPrimaryPan(panMasked);
        req.setUpdatedOn(LocalDateTime.now());
        cardRequestRepository.save(req);
        //Format Setting here.

        String accountNum = req.getAccountNum();
        if (accountNum != null && !accountNum.isBlank()) {
            boolean alreadyLinked = cardAccountRepository.findByCardId(saved.getCardId()).stream()
                .anyMatch(ca -> accountNum.equals(ca.getAccountNum()));
            if (!alreadyLinked) {
                var account = accountRepository.findByAccountNum(accountNum)
                    .orElseThrow(() -> new ResourceNotFoundException("Account", accountNum));
                CardAccount ca = new CardAccount();
                ca.setCardId(saved.getCardId());
                ca.setPan(panMasked);
                ca.setAccountNum(accountNum);
                ca.setAccountId(account.getAccountId());
                ca.setRelationshipNum(req.getRelationshipNum() != null ? req.getRelationshipNum() : "");
                ca.setEffectiveFrom(LocalDateTime.now());
                ca.setEffectiveTo(LocalDateTime.now().plusYears(50));
                ca.setIsOverallDefault(1);
                ca.setIsAcctTypeDefault(0);
                ca.setCreatedOn(LocalDateTime.now());
                ca.setUpdatedOn(LocalDateTime.now());
                ca.setCreatedBy("system");
                ca.setUpdatedBy("system");
                cardAccountRepository.save(ca);
            }
        }

        result.setSuccess(true);
        result.setMessage("Card generated successfully");
        result.setCardId(saved.getCardId());
        result.setPanMasked(panMasked);
        return result;
    }

    /**
     * When a card request originated from the mobile app (requestTypeId NEW / 1 / MOBILE),
     * assign the configured standard limit profile if the card does not already have one.
     * DB column LIMIT_PROFILE is NUMBER — store LimitProfile.id (e.g. 3), not code "STD".
     */
    private void applyMobileStandardLimitIfNeeded(CardRequest req, Card card) {
        if (!isMobileCardRequest(req)) {
            return;
        }
        if (card.getLimitProfile() != null && !card.getLimitProfile().isBlank()) {
            return;
        }
        String configured = mobileDefaultLimitProfile != null ? mobileDefaultLimitProfile.trim() : "STD";
        if (configured.isEmpty()) {
            return;
        }
        Optional<LimitProfile> profile = resolveLimitProfile(configured);
        if (profile.isEmpty()) {
            log.warn("Mobile card request {}: limit profile '{}' not found; card generated without limit",
                req.getRequestId(), configured);
            return;
        }
        LimitProfile lp = profile.get();
        card.setLimitProfile(String.valueOf(lp.getId()));
        card.setLimitProfileId(lp.getId());
        log.info("Assigned limit profile id {} (code {}) to card from mobile request {}",
            lp.getId(), lp.getProfileCode(), req.getRequestId());
    }

    private Optional<LimitProfile> resolveLimitProfile(String configured) {
        if (configured.matches("\\d+")) {
            return limitProfileRepository.findById(Long.parseLong(configured));
        }
        return limitProfileRepository.findByProfileCode(configured);
    }

    private boolean isMobileCardRequest(CardRequest req) {
        if (req == null || req.getRequestTypeId() == null || req.getRequestTypeId().isBlank()) {
            return false;
        }
        return MOBILE_REQUEST_TYPES.contains(req.getRequestTypeId().trim().toUpperCase());
    }

    private Optional<Card> findReplacementTargetCard(CardRequest req) {
        List<Card> candidates = cardRepository.findByRelationshipNumAndCardStatusCode(req.getRelationshipNum(), "WARM");
        return candidates.stream()
            .filter(c -> c.getIsReplaced() != null && c.getIsReplaced() == 1)
            .filter(c -> req.getAccountNum() == null || req.getAccountNum().isBlank() ||
                cardAccountRepository.findByCardId(c.getCardId()).stream()
                    .anyMatch(ca -> req.getAccountNum().equals(ca.getAccountNum())))
            .max(Comparator.comparing(Card::getUpdatedOn, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private String resolvePan(Card card) {
        if (card == null) return null;
        if (card.getPanEncrypted() != null && !card.getPanEncrypted().isBlank()) {
            try {
                return encryptionService.decrypt(card.getPanEncrypted());
            } catch (RuntimeException ignore) {
                // Fall through to legacy plain PAN if encrypted value is not readable.
            }
        }
        // Masked PAN in clear column is not usable as full PAN
        String pan = card.getPan();
        if (pan != null && pan.contains("*")) return null;
        return pan;
    }

    // Modified this code for PAN generation
    private String generatePan(Long cardTypeId, String cardTypeCode) {
        int bin = 900419;
        if (cardTypeId != null) {
            var cardType = cardTypeRepository.findById(cardTypeId).orElse(null);
            if (cardType != null && cardType.getBin() != null) {
                bin = cardType.getBin();
            }
        } else if (cardTypeCode != null) {
            var cardType = cardTypeRepository.findByCardTypeCode(cardTypeCode).orElse(null);
            if (cardType != null && cardType.getBin() != null) {
                bin = cardType.getBin();
            }
        }
        String binStr = String.format("%06d", bin);
        Random random = new Random();
        StringBuilder middle = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            middle.append(random.nextInt(10));
        }
        String pan15 = binStr + middle;
        int luhn = calculateLuhnDigit(pan15);
        return pan15 + luhn;
    }

    private int calculateLuhnDigit(String pan15) {
        int sum = 0;
        boolean alternate = true;
        for (int i = pan15.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(String.valueOf(pan15.charAt(i)));
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return (10 - (sum % 10)) % 10;
    }
    // Modified this code for PAN generation

    @Override
    @Transactional
    public void updateCardRequestProgress(Long requestId, Integer progressFlag) {
        CardRequest req = cardRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("CardRequest", String.valueOf(requestId)));
        req.setProgressFlag(progressFlag);
        req.setUpdatedOn(LocalDateTime.now());
        cardRequestRepository.save(req);
    }
    @Override
    @Transactional
    public CardGenerationResultResponse approveAndGenerate(Long requestId) {
        CardRequest req = cardRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("CardRequest", String.valueOf(requestId)));
        if (req.getIsProcessed() != null && req.getIsProcessed() == 1) {
            CardGenerationResultResponse already = new CardGenerationResultResponse();
            already.setSuccess(false);
            already.setMessage("Request already processed");
            return already;
        }
        updateCardRequestProgress(requestId, 1);
        return processNewCardGeneration(requestId);
    }
}
