package com.cms.app.service;

import com.cms.app.config.AESencryption;
import com.cms.app.config.ResponseCode;
import com.cms.app.security.AuthContext;
import com.cms.app.entity.Account;
import com.cms.app.entity.AccountStatus;
import com.cms.app.entity.AccountType;
import com.cms.app.entity.Branch;
import com.cms.app.entity.Card;
import com.cms.app.entity.CardLimitActual;
import com.cms.app.entity.CardLimitCustomized;
import com.cms.app.entity.CardProduct;
import com.cms.app.entity.CardProductionStatus;
import com.cms.app.entity.CardRequest;
import com.cms.app.entity.CardStatus;
import com.cms.app.entity.CardTransaction;
import com.cms.app.entity.CardType;
import com.cms.app.entity.LimitProfile;
import com.cms.app.repository.AccountRepository;
import com.cms.app.repository.AccountStatusRepository;
import com.cms.app.repository.AccountTypeRepository;
import com.cms.app.repository.BranchRepository;
import com.cms.app.repository.CardLimitActualRepository;
import com.cms.app.repository.CardLimitCustomizedRepository;
import com.cms.app.repository.CardProductRepository;
import com.cms.app.repository.CardProductionStatusRepository;
import com.cms.app.repository.CardRepository;
import com.cms.app.repository.CardRequestRepository;
import com.cms.app.repository.CardSpendingSummaryRepository;
import com.cms.app.repository.CardStatusRepository;
import com.cms.app.repository.CardTransactionRepository;
import com.cms.app.repository.CardTypeRepository;
import com.cms.app.repository.LimitProfileRepository;
import com.cms.app.request.CardAuthorizeRequest;
import com.cms.app.request.CardAvailableLimitRequest;
import com.cms.app.request.CardInquiryRequest;
import com.cms.app.request.CardLimitValidateRequest;
import com.cms.app.request.CardNewRequest;
import com.cms.app.request.CardSpendingSummaryRequest;
import com.cms.app.request.CardStatementRequest;
import com.cms.app.request.CardTransactionHistoryRequest;
import com.cms.app.request.CardUpdateStatusRequest;
import com.cms.app.request.CardValidationRequest;
import com.cms.app.request.ChangePinRequest;
import com.cms.app.request.ForgotPin;
import com.cms.app.response.CardAuthorizeResponse;
import com.cms.app.response.CardInquiryResponse;
import com.cms.app.response.CardLimitValidateResponse;
import com.cms.app.response.CardLovResponse;
import com.cms.app.response.CardSpendingSummaryResponse;
import com.cms.app.response.CardStatementResponse;
import com.cms.app.response.CardTransactionHistoryResponse;
import com.cms.app.response.CardTransactionItemResponse;
import com.cms.app.response.ResponseWrapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CardService {

    private static final String DEFAULT_ACCOUNT_TYPE = "00";
    private static final String DEFAULT_ACCOUNT_STATUS = "OPEN";
    private static final String DEFAULT_CURRENCY = "AFN";
    /** VCR-007 / FRD-069: max failed PIN/CVV/track2 attempts before PIN lock. */
    private static final int DEFAULT_PIN_MAX_RETRY = 3;
    private static final int PIN_STATUS_OK = 0;
    private static final int PIN_STATUS_LOCKED = 1;

    private final CardRepository cardRepository;
    private final CardRequestRepository cardRequestRepository;
    private final AccountRepository accountRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final AccountStatusRepository accountStatusRepository;
    private final CardProductRepository cardProductRepository;
    private final CardTypeRepository cardTypeRepository;
    private final CardStatusRepository cardStatusRepository;
    private final BranchRepository branchRepository;
    private final LimitProfileRepository limitProfileRepository;
    private final CardProductionStatusRepository cardProductionStatusRepository;
    private final CardSpendingSummaryRepository cardSpendingSummaryRepository;
    private final CardLimitActualRepository cardLimitActualRepository;
    private final CardLimitCustomizedRepository cardLimitCustomizedRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final AESencryption aesEncryption;
    private final PinBlockService pinBlockService;
    private final CardDataEncryptionService cardDataEncryptionService;
    private final OtpProofService otpProofService;

    public CardService(CardRepository cardRepository,
                       CardRequestRepository cardRequestRepository,
                       AccountRepository accountRepository,
                       AccountTypeRepository accountTypeRepository,
                       AccountStatusRepository accountStatusRepository,
                       CardProductRepository cardProductRepository,
                       CardTypeRepository cardTypeRepository,
                       CardStatusRepository cardStatusRepository,
                       BranchRepository branchRepository,
                       LimitProfileRepository limitProfileRepository,
                       CardProductionStatusRepository cardProductionStatusRepository,
                       CardSpendingSummaryRepository cardSpendingSummaryRepository,
                       CardLimitActualRepository cardLimitActualRepository,
                       CardLimitCustomizedRepository cardLimitCustomizedRepository,
                       CardTransactionRepository cardTransactionRepository,
                       AESencryption aesEncryption,
                       PinBlockService pinBlockService,
                       CardDataEncryptionService cardDataEncryptionService,
                       OtpProofService otpProofService) {
        this.cardRepository = cardRepository;
        this.cardRequestRepository = cardRequestRepository;
        this.accountRepository = accountRepository;
        this.accountTypeRepository = accountTypeRepository;
        this.accountStatusRepository = accountStatusRepository;
        this.cardProductRepository = cardProductRepository;
        this.cardTypeRepository = cardTypeRepository;
        this.cardStatusRepository = cardStatusRepository;
        this.branchRepository = branchRepository;
        this.limitProfileRepository = limitProfileRepository;
        this.cardProductionStatusRepository = cardProductionStatusRepository;
        this.cardSpendingSummaryRepository = cardSpendingSummaryRepository;
        this.cardLimitActualRepository = cardLimitActualRepository;
        this.cardLimitCustomizedRepository = cardLimitCustomizedRepository;
        this.cardTransactionRepository = cardTransactionRepository;
        this.aesEncryption = aesEncryption;
        this.pinBlockService = pinBlockService;
        this.cardDataEncryptionService = cardDataEncryptionService;
        this.otpProofService = otpProofService;
    }

    @Transactional
    public ResponseWrapper<List<CardInquiryResponse>> inquiry(CardInquiryRequest request) {
        ResponseWrapper<List<CardInquiryResponse>> response = new ResponseWrapper<>();
        response.setResponseCode(ResponseCode.NO_DATA);
        response.setResponseMessage(ResponseCode.getMessage(ResponseCode.NO_DATA));

        String relationshipNum = request.getRelationshipNum();
        if (!relationshipAllowed(relationshipNum)) {
            response.setResponseCode(ResponseCode.FORBIDDEN);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.FORBIDDEN));
            return response;
        }
        String cardType = blankToNull(request.getCardType());

        if (cardType != null) {
            Optional<CardRequest> pending = cardRequestRepository
                    .findTopByRelationshipNumAndCardTypeCodeAndIsProcessedOrderByCreatedOnDesc(
                            relationshipNum, cardType, 0);
            if (pending.isPresent()) {
                response.setResponseCode(ResponseCode.CARD_IN_PROCESS);
                response.setResponseMessage(ResponseCode.getMessage(ResponseCode.CARD_IN_PROCESS));
                return response;
            }
            Optional<Card> card = cardRepository
                    .findFirstByRelationshipNumAndCardTypeCodeOrderByCreatedOnDesc(relationshipNum, cardType);
            if (card.isEmpty()) {
                return response;
            }
            response.setResponseCode(ResponseCode.SUCCESS);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.SUCCESS));
            response.setResponseBody(List.of(toInquiryResponse(card.get(), request.getPin())));
            return response;
        }

        List<Card> cards = cardRepository.findByRelationshipNumOrderByCreatedOnDesc(relationshipNum);
        if (cards.isEmpty()) {
            Optional<CardRequest> pending = cardRequestRepository
                    .findTopByRelationshipNumAndIsProcessedOrderByCreatedOnDesc(relationshipNum, 0);
            if (pending.isPresent()) {
                response.setResponseCode(ResponseCode.CARD_IN_PROCESS);
                response.setResponseMessage(ResponseCode.getMessage(ResponseCode.CARD_IN_PROCESS));
            }
            return response;
        }

        response.setResponseCode(ResponseCode.SUCCESS);
        response.setResponseMessage(ResponseCode.getMessage(ResponseCode.SUCCESS));
        String pin = request.getPin();
        response.setResponseBody(cards.stream().map(c -> toInquiryResponse(c, pin)).toList());
        return response;
    }

    /**
     * Physical: CVV only when optional PIN matches (option A — wrong/missing PIN still returns the card list).
     * Wrong PIN on reveal consumes a try; locked PIN never reveals. Virtual: always full PAN/CVV.
     */
    private CardInquiryResponse toInquiryResponse(Card card, String requestPin) {
        CardInquiryResponse body = new CardInquiryResponse();
        body.setCardTitle(card.getCardTitle());
        body.setCreatedOn(card.getCreatedOn());
        body.setExpiryDate(card.getExpiryDate());

        boolean virtual = isVirtualCard(card);
        body.setIsVirtual(virtual);
        if (virtual) {
            String clearPan = decryptPan(card);
            body.setPan(clearPan != null ? clearPan : card.getPan());
            body.setCvv(cardDataEncryptionService.decryptSensitiveField(card.getCvv()));
            body.setCvv2(cardDataEncryptionService.decryptSensitiveField(card.getCvv2()));
            body.setPinSet(false);
            body.setSensitiveRevealed(true);
        } else {
            boolean pinSet = card.getPinOffset() != null && !card.getPinOffset().isBlank();
            body.setPinSet(pinSet);
            body.setPan(card.getPan());
            boolean reveal = false;
            if (pinSet && requestPin != null && !requestPin.isBlank()) {
                if (isPinLocked(card)) {
                    reveal = false;
                } else if (pinsMatchFlexible(card.getPinOffset(), requestPin)) {
                    resetPinRetries(card);
                    reveal = true;
                } else {
                    recordCredentialFailure(card);
                    reveal = false;
                }
            }
            if (reveal) {
                body.setCvv(decodeMaybe(card.getCvv()));
                body.setCvv2(decodeMaybe(card.getCvv2()));
                body.setSensitiveRevealed(true);
            } else {
                body.setCvv(null);
                body.setCvv2(null);
                body.setSensitiveRevealed(false);
            }
        }

        if (card.getCardStatusCode() != null) {
            cardStatusRepository.findByCardStatusCode(card.getCardStatusCode())
                    .map(CardStatus::getCardStatusName)
                    .ifPresentOrElse(body::setCardStatusCode, () -> body.setCardStatusCode(card.getCardStatusCode()));
        }
        if (card.getCardTypeCode() != null) {
            body.setCardTypeCode(card.getCardTypeCode());
            cardTypeRepository.findByCardTypeCode(card.getCardTypeCode())
                    .map(CardType::getCardTypeName)
                    .ifPresentOrElse(body::setCardTypeName, () -> body.setCardTypeName(card.getCardTypeCode()));
        }
        if (card.getCardProdStatusId() != null) {
            cardProductionStatusRepository.findByCardProdStatusId(card.getCardProdStatusId())
                    .map(CardProductionStatus::getCardProdStatusName)
                    .ifPresentOrElse(body::setCardProdStatus, () -> body.setCardProdStatus(card.getCardProdStatusId()));
        }
        return body;
    }

    private String decodeMaybe(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return cardDataEncryptionService.decryptSensitiveField(value);
    }

    private String decryptPan(Card card) {
        if (card.getPanEncrypted() == null || card.getPanEncrypted().isBlank()) {
            return null;
        }
        try {
            return cardDataEncryptionService.decrypt(card.getPanEncrypted());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean isVirtualCard(Card card) {
        if (card == null || card.getCardTypeCode() == null || card.getCardTypeCode().isBlank()) {
            return false;
        }
        return cardTypeRepository.findByCardTypeCode(card.getCardTypeCode().trim())
                .map(CardType::isVirtualCard)
                .orElse(false);
    }

    /** Resolve by clear PAN column first, then by PAN_HASH (full PAN from virtual inquiry). */
    private Optional<Card> findCardByPanInput(String panInput) {
        if (panInput == null || panInput.isBlank()) {
            return Optional.empty();
        }
        String pan = panInput.trim();
        Optional<Card> byPan = cardRepository.findByPan(pan);
        if (byPan.isPresent()) {
            return byPan;
        }
        String hash = cardDataEncryptionService.panHashForLookup(pan);
        if (hash == null) {
            return Optional.empty();
        }
        return cardRepository.findByPanHash(hash);
    }

    private ResponseWrapper<Void> pinNotApplicableForVirtual() {
        ResponseWrapper<Void> response = new ResponseWrapper<>();
        response.setResponseCode(ResponseCode.PIN_NOT_APPLICABLE_VIRTUAL);
        response.setResponseMessage(ResponseCode.getMessage(ResponseCode.PIN_NOT_APPLICABLE_VIRTUAL));
        return response;
    }

    /**
     * Like old cms: create ACCOUNT if missing, then insert CARD_REQUEST.
     * No CUSTOMER_ACCOUNT (table not on portal schema).
     */
    @Transactional
    public ResponseWrapper<Void> newRequest(CardNewRequest request) {
        ResponseWrapper<Void> response = new ResponseWrapper<>();

        Optional<CardProduct> product = cardProductRepository.findByProductCode(request.getProductCode());
        if (product.isEmpty()) {
            response.setResponseCode(ResponseCode.INVALID_PRODUCT);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.INVALID_PRODUCT));
            return response;
        }

        Optional<CardType> cardType = cardTypeRepository.findByCardTypeCode(request.getCardType());
        if (cardType.isEmpty()) {
            response.setResponseCode(ResponseCode.INVALID_CARD_TYPE);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.INVALID_CARD_TYPE));
            return response;
        }

        String relationshipNum = request.getRelationshipNum();
        if (!relationshipAllowed(relationshipNum)) {
            response.setResponseCode(ResponseCode.FORBIDDEN);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.FORBIDDEN));
            return response;
        }
        String cardTypeCode = request.getCardType().trim();

        Optional<CardRequest> pending = cardRequestRepository
                .findTopByRelationshipNumAndCardTypeCodeAndIsProcessedOrderByCreatedOnDesc(
                        relationshipNum, cardTypeCode, 0);
        if (pending.isPresent()) {
            response.setResponseCode(ResponseCode.CARD_REQUEST_ALREADY_PENDING);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.CARD_REQUEST_ALREADY_PENDING));
            return response;
        }

        Optional<Card> existing = cardRepository
                .findFirstByRelationshipNumAndCardTypeCodeOrderByCreatedOnDesc(relationshipNum, cardTypeCode);
        if (existing.isPresent()) {
            response.setResponseCode(ResponseCode.ALREADY_DATA);
            response.setResponseMessage("A card of this type already exists for this relationship");
            return response;
        }

        Account account = accountRepository.findByAccountNum(request.getAccountNumber())
                .orElseGet(() -> createAccount(request));
        if (account == null) {
            response.setResponseCode(ResponseCode.FAILURE);
            response.setResponseMessage("Unable to create account (missing account type/status/branch lookup)");
            return response;
        }

        String branchCode = resolveBranchCode(account);

        CardRequest cr = new CardRequest();
        cr.setRelationshipNum(relationshipNum);
        cr.setAccountNum(request.getAccountNumber());
        cr.setCardTitle(request.getCardTitle());
        cr.setCardTypeCode(cardTypeCode);
        cr.setProductCode(request.getProductCode());
        cr.setBranchCode(branchCode);
        cr.setRequestTypeId(request.getRequestTypeId());
        cr.setSupplementaryCount(0);
        cr.setIsProcessed(0);
        cr.setProgressFlag(0);
        cr.setCreatedBy("API_APP");
        cr.setUpdatedBy("API_APP");
        cr.setCreatedOn(LocalDateTime.now());
        cr.setUpdatedOn(LocalDateTime.now());
        cardRequestRepository.save(cr);

        response.setResponseCode(ResponseCode.SUCCESS);
        response.setResponseMessage(ResponseCode.getMessage(ResponseCode.SUCCESS));
        return response;
    }

    /** Create ACCOUNT like old cms when accountNumber does not exist yet. */
    private Account createAccount(CardNewRequest request) {
        Optional<AccountType> typeOpt = accountTypeRepository.findByAcctTypeCode(DEFAULT_ACCOUNT_TYPE);
        if (typeOpt.isEmpty()) {
            typeOpt = accountTypeRepository.findAll().stream().findFirst();
        }
        Optional<AccountStatus> statusOpt = accountStatusRepository.findByAcctStatusCode(DEFAULT_ACCOUNT_STATUS);
        if (statusOpt.isEmpty()) {
            statusOpt = accountStatusRepository.findByAcctStatusCode("10");
        }
        if (statusOpt.isEmpty()) {
            statusOpt = accountStatusRepository.findAll().stream().findFirst();
        }
        Optional<Branch> branchOpt = branchRepository.findByBranchCode("1");
        if (branchOpt.isEmpty()) {
            branchOpt = branchRepository.findAll().stream().findFirst();
        }
        if (typeOpt.isEmpty() || statusOpt.isEmpty() || branchOpt.isEmpty()) {
            return null;
        }

        AccountType type = typeOpt.get();
        AccountStatus status = statusOpt.get();
        Branch branch = branchOpt.get();
        LocalDateTime now = LocalDateTime.now();

        Account account = new Account();
        account.setAccountNum(request.getAccountNumber());
        account.setAccountTitle(request.getCardTitle());
        account.setAcctTypeCode(type.getAcctTypeCode());
        account.setAccountTypeId(type.getId());
        account.setAcctStatusCode(status.getAcctStatusCode());
        account.setAccountStatusId(status.getId());
        account.setBranchCode(branch.getBranchCode());
        account.setBranchId(branch.getId());
        account.setCurrencyCode(branch.getCurrencyCode() != null && !branch.getCurrencyCode().isBlank()
                ? branch.getCurrencyCode() : DEFAULT_CURRENCY);
        account.setIsJoint(BigDecimal.ZERO);
        account.setIsClosed(Boolean.FALSE);
        account.setOpenedDate(now);
        account.setCreatedOn(now);
        account.setUpdatedOn(now);
        account.setCreatedBy("API_APP");
        account.setUpdatedBy("API_APP");
        return accountRepository.save(account);
    }

    private String resolveBranchCode(Account account) {
        if (account.getBranchCode() != null && !account.getBranchCode().isBlank()) {
            return account.getBranchCode();
        }
        return branchRepository.findAll().stream()
                .findFirst()
                .map(Branch::getBranchCode)
                .orElse("1");
    }

    @Transactional
    public ResponseWrapper<Void> updateStatus(CardUpdateStatusRequest request) {
        ResponseWrapper<Void> response = new ResponseWrapper<>();
        response.setResponseCode(ResponseCode.NO_DATA);
        response.setResponseMessage(ResponseCode.getMessage(ResponseCode.NO_DATA));

        Optional<Card> cardOpt = cardRepository.findByPan(request.getPan());
        if (cardOpt.isEmpty()) {
            return response;
        }
        Card card = cardOpt.get();
        if (!relationshipAllowed(card.getRelationshipNum())) {
            response.setResponseCode(ResponseCode.FORBIDDEN);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.FORBIDDEN));
            return response;
        }
        if (card.getCardStatusCode() != null && "003".equals(card.getCardStatusCode().trim())) {
            response.setResponseCode(ResponseCode.CARD_STATUS_UPDATE_ERROR);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.CARD_STATUS_UPDATE_ERROR));
            return response;
        }

        Optional<CardStatus> status = cardStatusRepository.findByCardStatusCode(request.getStatusCode());
        if (status.isEmpty()) {
            return response;
        }

        card.setCardStatusCode(status.get().getCardStatusCode());
        card.setUpdatedOn(LocalDateTime.now());
        card.setUpdatedBy("API_APP");
        cardRepository.save(card);

        response.setResponseCode(ResponseCode.SUCCESS);
        response.setResponseMessage(ResponseCode.getMessage(ResponseCode.SUCCESS));
        return response;
    }

    public ResponseWrapper<List<CardLovResponse>> getLov(String slug) {
        ResponseWrapper<List<CardLovResponse>> response = new ResponseWrapper<>();
        List<CardLovResponse> lovs = new ArrayList<>();
        String key = slug == null ? "" : slug.trim().toLowerCase();
        if (key.isEmpty() || "all".equals(key)) {
            lovs.add(cardProducts());
            lovs.add(cardTypes());
            lovs.add(cardStatuses());
            lovs.add(cardProductionStatuses());
            lovs.add(cardRequestTypes());
        } else if ("card-product".equals(key) || "card-products".equals(key) || "product".equals(key)) {
            lovs.add(cardProducts());
        } else if ("card-types".equals(key) || "card-type".equals(key) || "type".equals(key)) {
            lovs.add(cardTypes());
        } else if ("card-status".equals(key) || "card-statuses".equals(key) || "status".equals(key)) {
            lovs.add(cardStatuses());
        } else if ("card-prod-status".equals(key) || "card-production-status".equals(key)
                || "production-status".equals(key)) {
            lovs.add(cardProductionStatuses());
        } else if ("card-request-types".equals(key) || "request-types".equals(key)) {
            lovs.add(cardRequestTypes());
        } else {
            response.setResponseCode(ResponseCode.NO_DATA);
            response.setResponseMessage("Unknown LOV slug: " + slug);
            response.setResponseBody(List.of());
            return response;
        }
        response.setResponseCode(ResponseCode.SUCCESS);
        response.setResponseMessage(ResponseCode.getMessage(ResponseCode.SUCCESS));
        response.setResponseBody(lovs);
        return response;
    }

    private CardLovResponse cardProducts() {
        CardLovResponse lov = new CardLovResponse();
        lov.setType("card-product");
        HashMap<String, String> map = new HashMap<>();
        for (CardProduct p : cardProductRepository.findAll()) {
            if (p == null || p.getProductCode() == null || p.getProductCode().isBlank()) {
                continue;
            }
            if (p.getIsActive() != null && p.getIsActive() != 1) {
                continue;
            }
            map.put(p.getProductCode(), p.getProductName() != null ? p.getProductName() : p.getProductCode());
        }
        if (map.isEmpty()) {
            for (CardProduct p : cardProductRepository.findAll()) {
                if (p == null || p.getProductCode() == null || p.getProductCode().isBlank()) {
                    continue;
                }
                map.put(p.getProductCode(), p.getProductName() != null ? p.getProductName() : p.getProductCode());
            }
        }
        lov.setLov(map);
        return lov;
    }

    private CardLovResponse cardTypes() {
        CardLovResponse lov = new CardLovResponse();
        lov.setType("card-types");
        HashMap<String, String> map = new HashMap<>();
        for (CardType t : cardTypeRepository.findAll()) {
            if (t == null || t.getCardTypeCode() == null || t.getCardTypeCode().isBlank()) {
                continue;
            }
            if (t.getIsActive() != null && t.getIsActive() != 1) {
                continue;
            }
            map.put(t.getCardTypeCode(), t.getCardTypeName() != null ? t.getCardTypeName() : t.getCardTypeCode());
        }
        if (map.isEmpty()) {
            for (CardType t : cardTypeRepository.findAll()) {
                if (t == null || t.getCardTypeCode() == null || t.getCardTypeCode().isBlank()) {
                    continue;
                }
                map.put(t.getCardTypeCode(), t.getCardTypeName() != null ? t.getCardTypeName() : t.getCardTypeCode());
            }
        }
        lov.setLov(map);
        return lov;
    }

    private CardLovResponse cardStatuses() {
        CardLovResponse lov = new CardLovResponse();
        lov.setType("card-status");
        HashMap<String, String> map = new HashMap<>();
        for (CardStatus s : cardStatusRepository.findAll()) {
            if (s == null || s.getCardStatusCode() == null || s.getCardStatusCode().isBlank()) {
                continue;
            }
            map.put(s.getCardStatusCode(), s.getCardStatusName() != null ? s.getCardStatusName() : s.getCardStatusCode());
        }
        lov.setLov(map);
        return lov;
    }

    private CardLovResponse cardRequestTypes() {
        CardLovResponse lov = new CardLovResponse();
        lov.setType("card-request-types");
        HashMap<String, String> map = new HashMap<>();
        map.put("1", "NEW");
        map.put("NEW", "NEW");
        map.put("REPLACEMENT", "REPLACEMENT");
        map.put("RENEWAL", "RENEWAL");
        lov.setLov(map);
        return lov;
    }

    private CardLovResponse cardProductionStatuses() {
        CardLovResponse lov = new CardLovResponse();
        lov.setType("card-prod-status");
        HashMap<String, String> map = new HashMap<>();
        for (CardProductionStatus s : cardProductionStatusRepository.findAll()) {
            if (s == null || s.getCardProdStatusId() == null || s.getCardProdStatusId().isBlank()) {
                continue;
            }
            map.put(
                    s.getCardProdStatusId(),
                    s.getCardProdStatusName() != null ? s.getCardProdStatusName() : s.getCardProdStatusId());
        }
        lov.setLov(map);
        return lov;
    }

    @Transactional
    public ResponseWrapper<Void> validate(CardValidationRequest request) {
        ResponseWrapper<Void> response = new ResponseWrapper<>();
        response.setResponseCode(ResponseCode.SUCCESS);
        response.setResponseMessage(ResponseCode.getMessage(ResponseCode.SUCCESS));

        Optional<Card> cardOpt = findCardByPanInput(request.getPan());
        if (cardOpt.isEmpty()) {
            response.setResponseCode(ResponseCode.NO_DATA);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.NO_DATA));
            return response;
        }
        Card card = cardOpt.get();
        if (card.getExpiryDate() != null && card.getExpiryDate().isBefore(LocalDateTime.now())) {
            response.setResponseCode(ResponseCode.CARD_EXPIRED);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.CARD_EXPIRED));
            return response;
        }

        boolean virtual = isVirtualCard(card);
        boolean checkingPin = request.getPin() != null && !request.getPin().isEmpty();
        boolean checkingCvv = request.getCvv() != null && !request.getCvv().isEmpty();
        boolean checkingTrack2 = request.getTrack2() != null && !request.getTrack2().isEmpty();
        boolean credentialCheck = (!virtual && checkingPin)
                || checkingCvv
                || (!virtual && checkingTrack2);

        if (credentialCheck && !virtual && isPinLocked(card)) {
            response.setResponseCode(ResponseCode.PIN_LOCKED);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.PIN_LOCKED));
            return response;
        }

        if (checkingPin) {
            if (virtual) {
                response.setResponseCode(ResponseCode.PIN_NOT_APPLICABLE_VIRTUAL);
                response.setResponseMessage(ResponseCode.getMessage(ResponseCode.PIN_NOT_APPLICABLE_VIRTUAL));
                return response;
            }
            if (card.getPinOffset() == null || card.getPinOffset().isBlank()) {
                response.setResponseCode(ResponseCode.NO_DATA);
                response.setResponseMessage(ResponseCode.getMessage(ResponseCode.NO_DATA));
                return response;
            }
            if (!pinsMatch(card.getPinOffset(), request.getPin())) {
                recordCredentialFailure(card);
                response.setResponseCode(ResponseCode.CARD_PIN_NOT_MATCHED);
                response.setResponseMessage(ResponseCode.getMessage(ResponseCode.CARD_PIN_NOT_MATCHED));
                return response;
            }
        }

        if (checkingCvv) {
            String stored = cardDataEncryptionService.decryptSensitiveField(card.getCvv());
            String stored2 = cardDataEncryptionService.decryptSensitiveField(card.getCvv2());
            if (!request.getCvv().equals(stored) && !request.getCvv().equals(stored2)
                    && !request.getCvv().equals(card.getCvv()) && !request.getCvv().equals(card.getCvv2())) {
                if (!virtual) {
                    recordCredentialFailure(card);
                }
                response.setResponseCode(ResponseCode.CVV_NOT_MATCHED);
                response.setResponseMessage(ResponseCode.getMessage(ResponseCode.CVV_NOT_MATCHED));
                return response;
            }
        }

        if (checkingTrack2) {
            if (virtual) {
                response.setResponseCode(ResponseCode.FAILURE);
                response.setResponseMessage("Track2 is not applicable for virtual cards");
                return response;
            }
            String track2 = decodeMaybe(card.getTrack2Data());
            if (!request.getTrack2().equals(track2) && !request.getTrack2().equals(card.getTrack2Data())) {
                recordCredentialFailure(card);
                response.setResponseCode(ResponseCode.TRACK2_NOT_MATCHED);
                response.setResponseMessage(ResponseCode.getMessage(ResponseCode.TRACK2_NOT_MATCHED));
                return response;
            }
        }

        if (credentialCheck && !virtual) {
            resetPinRetries(card);
        }
        return response;
    }

    /**
     * Same as old cms change-pin: verify old PIN, confirm new, store new encrypted value.
     */
    @Transactional
    public ResponseWrapper<Void> changePin(@Valid ChangePinRequest request) {
        ResponseWrapper<Void> response = new ResponseWrapper<>();

        if (!relationshipAllowed(request.getRelationshipNum())) {
            response.setResponseCode(ResponseCode.FORBIDDEN);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.FORBIDDEN));
            return response;
        }
        Optional<Card> cardOpt = cardRepository.findByRelationshipNumAndPan(request.getRelationshipNum(), request.getPan());
        if (cardOpt.isEmpty()) {
            response.setResponseCode(ResponseCode.NO_DATA);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.NO_DATA));
            return response;
        }
        Card card = cardOpt.get();
        if (isVirtualCard(card)) {
            return pinNotApplicableForVirtual();
        }
        if (card.getPinOffset() == null || card.getPinOffset().isBlank()) {
            response.setResponseCode(ResponseCode.NO_DATA);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.NO_DATA));
            return response;
        }
        if (isPinLocked(card)) {
            response.setResponseCode(ResponseCode.PIN_LOCKED);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.PIN_LOCKED));
            return response;
        }
        if (!pinsMatch(card.getPinOffset(), request.getOldPin())) {
            recordCredentialFailure(card);
            response.setResponseCode(ResponseCode.CARD_PIN_NOT_MATCHED);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.CARD_PIN_NOT_MATCHED));
            return response;
        }
        if (request.getNewPin() == null || !request.getNewPin().equals(request.getConfirmNewPin())) {
            response.setResponseCode(ResponseCode.PINS_NOT_MATCHED);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.PINS_NOT_MATCHED));
            return response;
        }

        card.setPinOffset(request.getNewPin());
        card.setPinGeneratedOn(LocalDateTime.now());
        applyPinRetryDefaults(card);
        card.setPinStatus(PIN_STATUS_OK);
        card.setPinRetryAvailable(resolvePinMaxRetry(card));
        card.setUpdatedOn(LocalDateTime.now());
        card.setUpdatedBy("API_APP");
        cardRepository.save(card);

        response.setResponseCode(ResponseCode.SUCCESS);
        response.setResponseMessage(ResponseCode.getMessage(ResponseCode.SUCCESS));
        return response;
    }

    /**
     * Same as old cms generate-pin / forgotPin:
     * flag=F → reset existing PIN; otherwise first-time set + status 001.
     * Virtual cards reject PIN entirely.
     */
    @Transactional
    public ResponseWrapper<Void> forgotPin(ForgotPin request) {
        ResponseWrapper<Void> response = new ResponseWrapper<>();

        if (request.getPin() == null || request.getConfirmPin() == null
                || !request.getPin().equals(request.getConfirmPin())) {
            response.setResponseCode(ResponseCode.PINS_NOT_MATCHED);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.PINS_NOT_MATCHED));
            return response;
        }

        if (!relationshipAllowed(request.getRelationshipNum())) {
            response.setResponseCode(ResponseCode.FORBIDDEN);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.FORBIDDEN));
            return response;
        }
        Optional<Card> cardOpt = cardRepository.findByRelationshipNumAndPan(request.getRelationshipNum(), request.getPan());
        if (cardOpt.isEmpty()) {
            response.setResponseCode(ResponseCode.NO_DATA);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.NO_DATA));
            return response;
        }
        Card card = cardOpt.get();
        if (isVirtualCard(card)) {
            return pinNotApplicableForVirtual();
        }

        boolean pinAlreadySet = card.getPinOffset() != null && !card.getPinOffset().isBlank();

        if (request.getFlag() != null && request.getFlag().equalsIgnoreCase("F")) {
            // LasPay verifies OTP; CMS requires otpProof before resetting PIN.
            if (request.getOtpProof() == null || request.getOtpProof().isBlank()) {
                response.setResponseCode(ResponseCode.OTP_PROOF_REQUIRED);
                response.setResponseMessage(ResponseCode.getMessage(ResponseCode.OTP_PROOF_REQUIRED));
                return response;
            }
            String proofError = otpProofService.validateForgotPinProof(
                    request.getOtpProof(), request.getRelationshipNum(), request.getPan());
            if (proofError != null) {
                response.setResponseCode(ResponseCode.OTP_PROOF_INVALID);
                response.setResponseMessage(proofError);
                return response;
            }
            if (!pinAlreadySet) {
                response.setResponseCode(ResponseCode.NO_DATA);
                response.setResponseMessage(ResponseCode.getMessage(ResponseCode.NO_DATA));
                return response;
            }
            // Forgot-PIN unlocks PIN after LasPay otpProof (VCR-007).
            card.setPinOffset(request.getPin());
            card.setPinGeneratedOn(LocalDateTime.now());
            applyPinRetryDefaults(card);
            card.setPinStatus(PIN_STATUS_OK);
            card.setPinRetryAvailable(resolvePinMaxRetry(card));
            card.setUpdatedOn(LocalDateTime.now());
            card.setUpdatedBy("API_APP");
            cardRepository.save(card);
        } else {
            // First-time Set PIN only — do not overwrite an existing PIN
            if (pinAlreadySet) {
                response.setResponseCode(ResponseCode.PIN_ALREADY_SET);
                response.setResponseMessage(ResponseCode.getMessage(ResponseCode.PIN_ALREADY_SET));
                return response;
            }
            card.setPinOffset(request.getPin());
            card.setPinGeneratedOn(LocalDateTime.now());
            card.setPinStatus(PIN_STATUS_OK);
            card.setPinMaxRetry(DEFAULT_PIN_MAX_RETRY);
            card.setPinRetryAvailable(DEFAULT_PIN_MAX_RETRY);
            card.setCardStatusCode("001");
            card.setUpdatedOn(LocalDateTime.now());
            card.setUpdatedBy("API_APP");
            cardRepository.save(card);
        }

        response.setResponseCode(ResponseCode.SUCCESS);
        response.setResponseMessage(ResponseCode.getMessage(ResponseCode.SUCCESS));
        return response;
    }

    /** Same compare style as old cms: decrypt(stored) equals decrypt(request). */
    private boolean pinsMatch(String storedEncrypted, String requestEncrypted) {
        String stored = aesEncryption.decrypt(storedEncrypted);
        String incoming = aesEncryption.decrypt(requestEncrypted);
        if (stored == null || incoming == null) {
            return false;
        }
        return stored.equals(incoming);
    }

    /**
     * VCR-007: shared try counter for PIN / CVV / track2 on physical cards.
     * Lock is PIN-only ({@code PIN_STATUS=1}); card status is not changed to Warm.
     */
    private boolean isPinLocked(Card card) {
        applyPinRetryDefaults(card);
        if (card.getPinStatus() != null && card.getPinStatus() == PIN_STATUS_LOCKED) {
            return true;
        }
        Integer avail = card.getPinRetryAvailable();
        return avail != null && avail <= 0;
    }

    private void applyPinRetryDefaults(Card card) {
        if (card.getPinMaxRetry() == null || card.getPinMaxRetry() <= 0) {
            card.setPinMaxRetry(DEFAULT_PIN_MAX_RETRY);
        }
        if (card.getPinRetryAvailable() == null) {
            card.setPinRetryAvailable(card.getPinMaxRetry());
        }
        if (card.getPinStatus() == null) {
            card.setPinStatus(PIN_STATUS_OK);
        }
    }

    private int resolvePinMaxRetry(Card card) {
        Integer max = card.getPinMaxRetry();
        return max != null && max > 0 ? max : DEFAULT_PIN_MAX_RETRY;
    }

    private void recordCredentialFailure(Card card) {
        applyPinRetryDefaults(card);
        int avail = card.getPinRetryAvailable() != null ? card.getPinRetryAvailable() : resolvePinMaxRetry(card);
        avail = Math.max(0, avail - 1);
        card.setPinRetryAvailable(avail);
        if (avail <= 0) {
            card.setPinStatus(PIN_STATUS_LOCKED);
            card.setPinRetryAvailable(0);
        }
        card.setUpdatedOn(LocalDateTime.now());
        card.setUpdatedBy("API_APP");
        cardRepository.save(card);
    }

    private void resetPinRetries(Card card) {
        applyPinRetryDefaults(card);
        int max = resolvePinMaxRetry(card);
        card.setPinMaxRetry(max);
        card.setPinRetryAvailable(max);
        card.setPinStatus(PIN_STATUS_OK);
        card.setUpdatedOn(LocalDateTime.now());
        card.setUpdatedBy("API_APP");
        cardRepository.save(card);
    }

    public ResponseWrapper<CardLimitValidateResponse> availableLimit(CardAvailableLimitRequest request) {
        return resolveLimit(request.getPan(), request.getChannelCode(), request.getTranCode(), null, false);
    }

    public ResponseWrapper<List<CardSpendingSummaryResponse>> spendingSummary(CardSpendingSummaryRequest request) {
        ResponseWrapper<List<CardSpendingSummaryResponse>> response = new ResponseWrapper<>();

        String key = firstNonBlank(request.getAccountNumber(), request.getPan());
        if (key == null) {
            response.setResponseCode(ResponseCode.FAILURE);
            response.setResponseMessage("accountNumber or pan is required");
            return response;
        }

        String channelCode = blankToNull(request.getChannelCode());
        String tranCode = blankToNull(request.getTranCode());

        List<Object[]> rows = cardSpendingSummaryRepository.findSpendingSummary(key, channelCode, tranCode);
        if (rows == null || rows.isEmpty()) {
            response.setResponseCode(ResponseCode.NO_DATA);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.NO_DATA));
            return response;
        }

        List<CardSpendingSummaryResponse> body = new ArrayList<>();
        for (Object[] row : rows) {
            CardSpendingSummaryResponse item = new CardSpendingSummaryResponse();
            item.setAccountNumber(asString(row[0]));
            String rowPan = asString(row[1]);
            item.setPan(rowPan);
            item.setCardTitle(asString(row[2]));
            item.setExpiryDate(asExpiryString(row[3]));
            item.setCardStatusName(asString(row[4]));
            item.setMaxLimit(asDouble(row[5]));
            item.setSingleTranLimit(asDouble(row[6]));
            item.setDailyAvailableSpending(asDouble(row[7]));
            item.setMonthlyAvailableSpending(asDouble(row[8]));

            // Overlay same-day CARD_LIMIT_ACTUAL (authorize writes channel/tran as "1"/"2"/"3")
            String panForActual = rowPan != null ? rowPan : key;
            Long channelLong = null;
            if (channelCode != null) {
                try {
                    channelLong = Long.valueOf(channelCode);
                } catch (NumberFormatException ignored) {
                    channelLong = null;
                }
            }
            BigDecimal actualAvail = findTodayActualAvailable(panForActual, channelLong, tranCode, false);
            if (actualAvail != null) {
                item.setDailyAvailableSpending(actualAvail.doubleValue());
                item.setMonthlyAvailableSpending(actualAvail.doubleValue());
            }
            body.add(item);
        }

        response.setResponseCode(ResponseCode.SUCCESS);
        response.setResponseMessage(ResponseCode.getMessage(ResponseCode.SUCCESS));
        response.setResponseBody(body);
        return response;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * When the JWT carries a relationshipNum, the request must use the same value.
     * Tokens without a bound relationship (service accounts) are not restricted here.
     */
    private boolean relationshipAllowed(String requestRelationshipNum) {
        String tokenRel = AuthContext.relationshipNum();
        if (tokenRel == null || tokenRel.isBlank()) {
            return true;
        }
        return requestRelationshipNum != null && tokenRel.equals(requestRelationshipNum.trim());
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Double asDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return new BigDecimal(value.toString()).doubleValue();
    }

    private static String asExpiryString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        if (value instanceof LocalDate ld) {
            return ld.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        if (value instanceof Date date) {
            return new Timestamp(date.getTime()).toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return String.valueOf(value);
    }

    public ResponseWrapper<CardLimitValidateResponse> validateLimit(CardLimitValidateRequest request) {
        return resolveLimit(request.getPan(), request.getChannelCode(), request.getTranCode(), request.getAmount(), true);
    }

    /**
     * Authorize a transaction amount:
     * 1) verify PAN / PIN / expiry
     * 2) ceiling from CARD_LIMIT_CUSTOMIZED (if active) else profile/max
     * 3) same-day CARD_LIMIT_ACTUAL remaining (create row if missing / new day)
     * 4) deduct amount and update the same actual row
     * 5) append CARD_TRANSACTION journal row (success or decline after card is known)
     *
     * channelCode: 1=ATM, 2=POS, 3=Ecommerce. tranCode defaults to same as channel.
     */
    @Transactional
    public ResponseWrapper<CardAuthorizeResponse> authorizeTransaction(CardAuthorizeRequest request) {
        ResponseWrapper<CardAuthorizeResponse> response = new ResponseWrapper<>();

        if (request == null || request.getPan() == null || request.getPan().isBlank()) {
            response.setResponseCode(ResponseCode.FAILURE);
            response.setResponseMessage("pan is required");
            return response;
        }
        if (request.getAmount() == null || request.getAmount().isBlank()) {
            response.setResponseCode(ResponseCode.INVALID_AMOUNT);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.INVALID_AMOUNT));
            return response;
        }
        if (!isKnownChannelCode(request.getChannelCode())) {
            response.setResponseCode(ResponseCode.FAILURE);
            response.setResponseMessage(CHANNEL_CODE_REQUIRED_MESSAGE);
            return response;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(request.getAmount().trim());
        } catch (NumberFormatException ex) {
            response.setResponseCode(ResponseCode.INVALID_AMOUNT);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.INVALID_AMOUNT));
            return response;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            response.setResponseCode(ResponseCode.INVALID_AMOUNT);
            response.setResponseMessage("amount must be greater than zero");
            return response;
        }

        String pan = request.getPan().trim();
        Optional<Card> cardOpt = findCardByPanInput(pan);
        if (cardOpt.isEmpty()) {
            response.setResponseCode(ResponseCode.NO_DATA);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.NO_DATA));
            return response;
        }
        Card card = cardOpt.get();
        // Limit actual rows are keyed by the PAN value stored on the card (often masked).
        String panForLimits = card.getPan() != null && !card.getPan().isBlank() ? card.getPan().trim() : pan;
        String channelKey = String.valueOf(request.getChannelCode());
        String tranKey = (request.getTranCode() != null && !request.getTranCode().isBlank())
                ? request.getTranCode().trim()
                : channelKey;
        String authRef = blankToNull(request.getAuthRef());

        if (card.getCardStatusCode() != null) {
            String status = card.getCardStatusCode().trim();
            if ("003".equals(status) || "HOT".equalsIgnoreCase(status)) {
                response.setResponseCode(ResponseCode.CARD_STATUS_UPDATE_ERROR);
                response.setResponseMessage(ResponseCode.getMessage(ResponseCode.CARD_STATUS_UPDATE_ERROR));
                recordAuthorizeTxn(card, panForLimits, amount, channelKey, tranKey, authRef, response, null);
                return response;
            }
            if ("002".equals(status) || "WARM".equalsIgnoreCase(status)) {
                response.setResponseCode(ResponseCode.CARD_TEMPORARILY_BLOCKED);
                response.setResponseMessage(ResponseCode.getMessage(ResponseCode.CARD_TEMPORARILY_BLOCKED));
                recordAuthorizeTxn(card, panForLimits, amount, channelKey, tranKey, authRef, response, null);
                return response;
            }
        }

        if (card.getExpiryDate() != null && card.getExpiryDate().isBefore(LocalDateTime.now())) {
            response.setResponseCode(ResponseCode.CARD_EXPIRED);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.CARD_EXPIRED));
            recordAuthorizeTxn(card, panForLimits, amount, channelKey, tranKey, authRef, response, null);
            return response;
        }

        if (request.getExpiryDate() != null && !request.getExpiryDate().isBlank()
                && card.getExpiryDate() != null
                && !expiryMatches(card.getExpiryDate(), request.getExpiryDate().trim())) {
            response.setResponseCode(ResponseCode.CARD_EXPIRED);
            response.setResponseMessage("Expiry date does not match card");
            recordAuthorizeTxn(card, panForLimits, amount, channelKey, tranKey, authRef, response, null);
            return response;
        }

        boolean virtual = isVirtualCard(card);
        boolean hasPinBlock = request.getPinBlock() != null && !request.getPinBlock().isBlank();
        boolean hasAesPin = request.getPin() != null && !request.getPin().isBlank();
        if (!virtual) {
            if (!hasPinBlock && !hasAesPin) {
                response.setResponseCode(ResponseCode.PIN_REQUIRED);
                response.setResponseMessage(ResponseCode.getMessage(ResponseCode.PIN_REQUIRED));
                recordAuthorizeTxn(card, panForLimits, amount, channelKey, tranKey, authRef, response, null);
                return response;
            }
            if (card.getPinOffset() == null || card.getPinOffset().isBlank()) {
                response.setResponseCode(ResponseCode.NO_DATA);
                response.setResponseMessage("PIN not set on card");
                recordAuthorizeTxn(card, panForLimits, amount, channelKey, tranKey, authRef, response, null);
                return response;
            }
            if (isPinLocked(card)) {
                response.setResponseCode(ResponseCode.PIN_LOCKED);
                response.setResponseMessage(ResponseCode.getMessage(ResponseCode.PIN_LOCKED));
                recordAuthorizeTxn(card, panForLimits, amount, channelKey, tranKey, authRef, response, null);
                return response;
            }
            if (hasPinBlock) {
                if (!pinBlockService.isReady()) {
                    response.setResponseCode(ResponseCode.FAILURE);
                    response.setResponseMessage("PIN block verification is not configured");
                    recordAuthorizeTxn(card, panForLimits, amount, channelKey, tranKey, authRef, response, null);
                    return response;
                }
                String clearPanForPin = decryptPan(card);
                if (clearPanForPin == null) {
                    clearPanForPin = pan;
                }
                if (!pinBlockService.matches(request.getPinBlock().trim(), clearPanForPin, card.getPinOffset())) {
                    recordCredentialFailure(card);
                    response.setResponseCode(ResponseCode.CARD_PIN_NOT_MATCHED);
                    response.setResponseMessage(ResponseCode.getMessage(ResponseCode.CARD_PIN_NOT_MATCHED));
                    recordAuthorizeTxn(card, panForLimits, amount, channelKey, tranKey, authRef, response, null);
                    return response;
                }
            } else if (!pinsMatchFlexible(card.getPinOffset(), request.getPin())) {
                recordCredentialFailure(card);
                response.setResponseCode(ResponseCode.CARD_PIN_NOT_MATCHED);
                response.setResponseMessage(ResponseCode.getMessage(ResponseCode.CARD_PIN_NOT_MATCHED));
                recordAuthorizeTxn(card, panForLimits, amount, channelKey, tranKey, authRef, response, null);
                return response;
            }
            resetPinRetries(card);
        }
        // Virtual: no CMS PIN — status + daily limit only (API key / JWT already enforced).

        CeilingResult ceiling = resolveAuthorizeCeiling(card, panForLimits, request.getChannelCode(), tranKey);
        if (ceiling.maxLimit == null || ceiling.maxLimit.compareTo(BigDecimal.ZERO) <= 0) {
            response.setResponseCode(ResponseCode.FAILURE);
            response.setResponseMessage("No daily limit configured for this channel");
            recordAuthorizeTxn(card, panForLimits, amount, channelKey, tranKey, authRef, response, null);
            return response;
        }

        if (amount.compareTo(ceiling.maxLimit) > 0) {
            response.setResponseCode(ResponseCode.CARD_LIMIT_EXCEED);
            response.setResponseMessage("Amount exceeds customized/max daily limit");
            CardAuthorizeResponse body = buildAuthorizeBody(
                    panForLimits, channelKey, tranKey, amount, ceiling.maxLimit, ceiling.maxLimit,
                    ceiling.customizedApplied, false);
            body.setAvailableLimit(null);
            response.setResponseBody(body);
            recordAuthorizeTxn(card, panForLimits, amount, channelKey, tranKey, authRef, response, null);
            return response;
        }

        LocalDate today = LocalDate.now();
        Optional<CardLimitActual> actualOpt =
                cardLimitActualRepository.findByPanAndChannelCodeAndTranCode(panForLimits, channelKey, tranKey);

        boolean created = false;
        CardLimitActual actual;
        BigDecimal remainingBefore;

        if (actualOpt.isPresent()
                && actualOpt.get().getCycleBeginDate() != null
                && actualOpt.get().getCycleBeginDate().equals(today)) {
            actual = actualOpt.get();
            remainingBefore = actual.getAvailableLimit() != null
                    ? actual.getAvailableLimit()
                    : ceiling.maxLimit;
        } else if (actualOpt.isPresent()) {
            actual = actualOpt.get();
            remainingBefore = ceiling.maxLimit;
            actual.setCycleBeginDate(today);
        } else {
            actual = new CardLimitActual();
            actual.setPan(panForLimits);
            actual.setChannelCode(channelKey);
            actual.setTranCode(tranKey);
            actual.setCycleBeginDate(today);
            actual.setAvailableTranCount(0L);
            actual.setCreatedOn(today);
            actual.setCreatedBy("API_APP");
            remainingBefore = ceiling.maxLimit;
            created = true;
        }

        if (amount.compareTo(remainingBefore) > 0) {
            response.setResponseCode(ResponseCode.CARD_LIMIT_EXCEED);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.CARD_LIMIT_EXCEED));
            CardAuthorizeResponse body = buildAuthorizeBody(
                    panForLimits, channelKey, tranKey, amount, ceiling.maxLimit, remainingBefore,
                    ceiling.customizedApplied, created);
            response.setResponseBody(body);
            recordAuthorizeTxn(card, panForLimits, amount, channelKey, tranKey, authRef, response, remainingBefore);
            return response;
        }

        BigDecimal remainingAfter = remainingBefore.subtract(amount);
        actual.setAvailableLimit(remainingAfter);
        if (actual.getAvailableTranCount() == null) {
            actual.setAvailableTranCount(0L);
        }
        actual.setUpdatedOn(today);
        actual.setUpdatedBy("API_APP");
        cardLimitActualRepository.save(actual);

        response.setResponseCode(ResponseCode.SUCCESS);
        response.setResponseMessage(ResponseCode.getMessage(ResponseCode.SUCCESS));
        response.setResponseBody(buildAuthorizeBody(
                panForLimits, channelKey, tranKey, amount, ceiling.maxLimit, remainingAfter,
                ceiling.customizedApplied, created));
        recordAuthorizeTxn(card, panForLimits, amount, channelKey, tranKey, authRef, response, remainingAfter);
        return response;
    }

    public ResponseWrapper<CardTransactionHistoryResponse> transactionHistory(CardTransactionHistoryRequest request) {
        ResponseWrapper<CardTransactionHistoryResponse> response = new ResponseWrapper<>();
        if (request == null) {
            response.setResponseCode(ResponseCode.FAILURE);
            response.setResponseMessage("request body is required");
            return response;
        }

        String relationshipNum = blankToNull(request.getRelationshipNum());
        if (relationshipNum == null && AuthContext.relationshipNum() != null) {
            relationshipNum = AuthContext.relationshipNum();
        }
        if (!relationshipAllowed(relationshipNum)) {
            response.setResponseCode(ResponseCode.FORBIDDEN);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.FORBIDDEN));
            return response;
        }

        String panFilter = resolveStoredPanFilter(request.getPan());
        if (request.getPan() != null && !request.getPan().isBlank() && panFilter == null) {
            response.setResponseCode(ResponseCode.NO_DATA);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.NO_DATA));
            return response;
        }
        if (panFilter != null) {
            Optional<Card> byPan = findCardByPanInput(request.getPan().trim());
            if (byPan.isPresent() && !relationshipAllowed(byPan.get().getRelationshipNum())) {
                response.setResponseCode(ResponseCode.FORBIDDEN);
                response.setResponseMessage(ResponseCode.getMessage(ResponseCode.FORBIDDEN));
                return response;
            }
        }
        if (relationshipNum == null && panFilter == null) {
            response.setResponseCode(ResponseCode.FAILURE);
            response.setResponseMessage("relationshipNum or pan is required");
            return response;
        }

        LocalDate from;
        LocalDate to;
        try {
            to = parseDateOrDefault(request.getToDate(), LocalDate.now());
            from = parseDateOrDefault(request.getFromDate(), to.minusDays(30));
        } catch (DateTimeParseException ex) {
            response.setResponseCode(ResponseCode.FAILURE);
            response.setResponseMessage("fromDate/toDate must be yyyy-MM-dd");
            return response;
        }
        if (from.isAfter(to)) {
            response.setResponseCode(ResponseCode.FAILURE);
            response.setResponseMessage("fromDate must be on or before toDate");
            return response;
        }

        int page = request.getPage() == null || request.getPage() < 0 ? 0 : request.getPage();
        int size = request.getSize() == null || request.getSize() < 1 ? 20 : Math.min(request.getSize(), 100);
        String channel = request.getChannelCode() == null ? null : String.valueOf(request.getChannelCode());

        Page<CardTransaction> result = cardTransactionRepository.search(
                relationshipNum,
                panFilter,
                channel,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                PageRequest.of(page, size));

        CardTransactionHistoryResponse body = new CardTransactionHistoryResponse();
        body.setPage(page);
        body.setSize(size);
        body.setTotalElements(result.getTotalElements());
        body.setTotalPages(result.getTotalPages());
        body.setItems(result.getContent().stream().map(this::toTxnItem).toList());

        response.setResponseCode(ResponseCode.SUCCESS);
        response.setResponseMessage(ResponseCode.getMessage(ResponseCode.SUCCESS));
        response.setResponseBody(body);
        return response;
    }

    public ResponseWrapper<CardStatementResponse> statement(CardStatementRequest request) {
        ResponseWrapper<CardStatementResponse> response = new ResponseWrapper<>();
        if (request == null) {
            response.setResponseCode(ResponseCode.FAILURE);
            response.setResponseMessage("request body is required");
            return response;
        }

        String relationshipNum = blankToNull(request.getRelationshipNum());
        if (relationshipNum == null && AuthContext.relationshipNum() != null) {
            relationshipNum = AuthContext.relationshipNum();
        }
        if (!relationshipAllowed(relationshipNum)) {
            response.setResponseCode(ResponseCode.FORBIDDEN);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.FORBIDDEN));
            return response;
        }

        String panFilter = resolveStoredPanFilter(request.getPan());
        if (request.getPan() != null && !request.getPan().isBlank() && panFilter == null) {
            response.setResponseCode(ResponseCode.NO_DATA);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.NO_DATA));
            return response;
        }
        if (panFilter != null) {
            Optional<Card> byPan = findCardByPanInput(request.getPan().trim());
            if (byPan.isPresent() && !relationshipAllowed(byPan.get().getRelationshipNum())) {
                response.setResponseCode(ResponseCode.FORBIDDEN);
                response.setResponseMessage(ResponseCode.getMessage(ResponseCode.FORBIDDEN));
                return response;
            }
        }
        if (relationshipNum == null && panFilter == null) {
            response.setResponseCode(ResponseCode.FAILURE);
            response.setResponseMessage("relationshipNum or pan is required");
            return response;
        }

        LocalDate from;
        LocalDate to;
        try {
            if (request.getMonth() != null && !request.getMonth().isBlank()) {
                YearMonth ym = YearMonth.parse(request.getMonth().trim());
                from = ym.atDay(1);
                to = ym.atEndOfMonth();
            } else {
                to = parseDateOrDefault(request.getToDate(), LocalDate.now());
                from = parseDateOrDefault(request.getFromDate(), to.withDayOfMonth(1));
            }
        } catch (DateTimeParseException ex) {
            response.setResponseCode(ResponseCode.FAILURE);
            response.setResponseMessage("month must be yyyy-MM; dates must be yyyy-MM-dd");
            return response;
        }
        if (from.isAfter(to)) {
            response.setResponseCode(ResponseCode.FAILURE);
            response.setResponseMessage("fromDate must be on or before toDate");
            return response;
        }

        String channel = request.getChannelCode() == null ? null : String.valueOf(request.getChannelCode());
        List<CardTransaction> rows = cardTransactionRepository.searchList(
                relationshipNum,
                panFilter,
                channel,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay());

        long approvedCount = 0;
        long declinedCount = 0;
        BigDecimal totalApproved = BigDecimal.ZERO;
        Map<String, BigDecimal> byChannel = new LinkedHashMap<>();
        List<CardTransactionItemResponse> items = new ArrayList<>();
        for (CardTransaction t : rows) {
            items.add(toTxnItem(t));
            boolean ok = t.getResponseCode() != null && t.getResponseCode() == ResponseCode.SUCCESS;
            if (ok) {
                approvedCount++;
                BigDecimal amt = t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO;
                totalApproved = totalApproved.add(amt);
                String ch = t.getChannelCode() != null ? t.getChannelCode() : "?";
                byChannel.merge(ch, amt, BigDecimal::add);
            } else {
                declinedCount++;
            }
        }

        CardStatementResponse body = new CardStatementResponse();
        body.setFromDate(from.toString());
        body.setToDate(to.toString());
        body.setRelationshipNum(relationshipNum);
        body.setPan(panFilter);
        body.setTotalCount(rows.size());
        body.setApprovedCount(approvedCount);
        body.setDeclinedCount(declinedCount);
        body.setTotalApprovedAmount(totalApproved);
        body.setApprovedAmountByChannel(byChannel);
        body.setTransactions(items);

        response.setResponseCode(ResponseCode.SUCCESS);
        response.setResponseMessage(ResponseCode.getMessage(ResponseCode.SUCCESS));
        response.setResponseBody(body);
        return response;
    }

    private void recordAuthorizeTxn(
            Card card,
            String panForLimits,
            BigDecimal amount,
            String channelKey,
            String tranKey,
            String authRef,
            ResponseWrapper<?> outcome,
            BigDecimal availableAfter) {
        try {
            CardTransaction txn = new CardTransaction();
            txn.setPan(panForLimits);
            txn.setPanHash(card.getPanHash());
            txn.setRelationshipNum(card.getRelationshipNum());
            txn.setAmount(amount);
            txn.setChannelCode(channelKey);
            txn.setTranCode(tranKey);
            txn.setResponseCode(outcome.getResponseCode());
            txn.setResponseMessage(outcome.getResponseMessage());
            txn.setAvailableAfter(availableAfter);
            txn.setAuthRef(authRef);
            txn.setCreatedOn(LocalDateTime.now());
            txn.setCreatedBy("API_APP");
            cardTransactionRepository.save(txn);
        } catch (RuntimeException ex) {
            // Do not fail authorize if journal insert has a problem
        }
    }

    private CardTransactionItemResponse toTxnItem(CardTransaction t) {
        CardTransactionItemResponse item = new CardTransactionItemResponse();
        item.setId(t.getId());
        item.setPan(t.getPan());
        item.setRelationshipNum(t.getRelationshipNum());
        item.setAmount(t.getAmount());
        item.setChannelCode(t.getChannelCode());
        item.setTranCode(t.getTranCode());
        item.setResponseCode(t.getResponseCode());
        item.setResponseMessage(t.getResponseMessage());
        item.setAvailableAfter(t.getAvailableAfter());
        item.setAuthRef(t.getAuthRef());
        item.setCreatedOn(t.getCreatedOn());
        item.setApproved(t.getResponseCode() != null && t.getResponseCode() == ResponseCode.SUCCESS);
        return item;
    }

    private String resolveStoredPanFilter(String panInput) {
        if (panInput == null || panInput.isBlank()) {
            return null;
        }
        return findCardByPanInput(panInput.trim())
                .map(c -> c.getPan() != null && !c.getPan().isBlank() ? c.getPan().trim() : panInput.trim())
                .orElse(null);
    }

    private static LocalDate parseDateOrDefault(String raw, LocalDate fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return LocalDate.parse(raw.trim());
    }

    private static final class CeilingResult {
        private final BigDecimal maxLimit;
        private final boolean customizedApplied;

        private CeilingResult(BigDecimal maxLimit, boolean customizedApplied) {
            this.maxLimit = maxLimit;
            this.customizedApplied = customizedApplied;
        }
    }

    private CeilingResult resolveAuthorizeCeiling(Card card, String pan, Long channelCode, String tranKey) {
        // 1) CARD_LIMIT_CUSTOMIZED — try numeric channel and named aliases
        for (String code : customizedLookupCodes(channelCode, tranKey)) {
            Optional<CardLimitCustomized> customized =
                    cardLimitCustomizedRepository.findByPanAndTranCodeAndIsActive(pan, code, 1);
            if (customized.isPresent() && customized.get().getCustomizedLimit() != null) {
                return new CeilingResult(customized.get().getCustomizedLimit(), true);
            }
        }

        // 2) CARD_LIMIT_PROFILE via spending summary MAX_LIMIT
        List<Object[]> rows = cardSpendingSummaryRepository.findSpendingSummary(
                pan, String.valueOf(channelCode), null);
        if (rows != null && !rows.isEmpty()) {
            Double maxVal = asDouble(rows.get(0)[5]);
            if (maxVal != null && maxVal > 0) {
                return new CeilingResult(BigDecimal.valueOf(maxVal), false);
            }
        }

        // 3) LIMIT_PROFILE daily by channel
        Long profileId = card.getLimitProfileId() != null ? card.getLimitProfileId() : card.getLimitProfile();
        if (profileId != null) {
            Optional<LimitProfile> profileOpt = limitProfileRepository.findById(profileId);
            if (profileOpt.isPresent()) {
                BigDecimal daily = pickChannelLimit(profileOpt.get(), channelCode);
                if (daily != null) {
                    return new CeilingResult(daily, false);
                }
            }
        }
        return new CeilingResult(null, false);
    }

    private static String[] customizedLookupCodes(Long channelCode, String tranKey) {
        java.util.LinkedHashSet<String> codes = new java.util.LinkedHashSet<>();
        if (tranKey != null && !tranKey.isBlank()) {
            codes.add(tranKey.trim());
            codes.add(tranKey.trim().toUpperCase());
        }
        if (channelCode != null) {
            codes.add(String.valueOf(channelCode));
            switch (channelCode.intValue()) {
                case 1 -> {
                    codes.add("ATM");
                    codes.add("WITHDRAWAL");
                    codes.add("WD");
                }
                case 2 -> codes.add("POS");
                case 3 -> {
                    codes.add("ECOM");
                    codes.add("ECOMMERCE");
                }
                default -> {
                }
            }
        }
        return codes.toArray(new String[0]);
    }

    private CardAuthorizeResponse buildAuthorizeBody(
            String pan,
            String channelKey,
            String tranKey,
            BigDecimal amount,
            BigDecimal maxLimit,
            BigDecimal available,
            boolean customizedApplied,
            boolean created) {
        CardAuthorizeResponse body = new CardAuthorizeResponse();
        body.setPan(pan);
        body.setChannelCode(channelKey);
        body.setTranCode(tranKey);
        body.setAmount(amount.doubleValue());
        body.setMaxLimit(maxLimit != null ? maxLimit.doubleValue() : null);
        body.setAvailableLimit(available != null ? available.doubleValue() : null);
        body.setCustomizedApplied(customizedApplied);
        body.setActualRowCreated(created);
        return body;
    }

    /** Accepts AES-encrypted PIN (mobile) or plaintext (Postman convenience). */
    private boolean pinsMatchFlexible(String storedEncrypted, String requestPin) {
        if (pinsMatch(storedEncrypted, requestPin)) {
            return true;
        }
        String stored = aesEncryption.decrypt(storedEncrypted);
        return stored != null && stored.equals(requestPin);
    }

    private boolean expiryMatches(LocalDateTime cardExpiry, String requestExpiry) {
        String raw = requestExpiry.trim();
        var slash = java.util.regex.Pattern.compile("^(\\d{1,2})\\s*/\\s*(\\d{2}|\\d{4})$").matcher(raw);
        if (!slash.matches()) {
            return false;
        }
        int mm = Integer.parseInt(slash.group(1));
        int yy = Integer.parseInt(slash.group(2));
        if (slash.group(2).length() == 2) {
            yy += 2000;
        }
        return cardExpiry.getMonthValue() == mm && cardExpiry.getYear() == yy;
    }

    private ResponseWrapper<CardLimitValidateResponse> resolveLimit(
            String pan, Long channelCode, Long tranCode, String amountStr, boolean consumeCheck) {
        ResponseWrapper<CardLimitValidateResponse> response = new ResponseWrapper<>();
        if (!isKnownChannelCode(channelCode)) {
            response.setResponseCode(ResponseCode.FAILURE);
            response.setResponseMessage(CHANNEL_CODE_REQUIRED_MESSAGE);
            return response;
        }
        Optional<Card> cardOpt = cardRepository.findByPan(pan);
        if (cardOpt.isEmpty()) {
            response.setResponseCode(ResponseCode.NO_DATA);
            response.setResponseMessage(ResponseCode.getMessage(ResponseCode.NO_DATA));
            return response;
        }
        Card card = cardOpt.get();

        String channelKey = String.valueOf(channelCode);
        String tranKey = tranCode != null ? String.valueOf(tranCode) : channelKey;

        // Same CARD_LIMIT_ACTUAL row authorize writes: PAN + channel + tran (tran defaults to channel).
        BigDecimal actualAvail = findTodayActualAvailable(pan, channelCode, tranKey, true);
        CeilingResult ceiling = resolveAuthorizeCeiling(card, pan, channelCode, tranKey);
        BigDecimal max = ceiling.maxLimit != null ? ceiling.maxLimit : BigDecimal.ZERO;
        BigDecimal available = actualAvail != null ? actualAvail : max;

        if (max.compareTo(BigDecimal.ZERO) <= 0 && available.compareTo(BigDecimal.ZERO) <= 0) {
            response.setResponseCode(ResponseCode.FAILURE);
            response.setResponseMessage("No daily limit configured for this channel");
            return response;
        }

        if (consumeCheck && amountStr != null && !amountStr.isBlank()) {
            BigDecimal amount = new BigDecimal(amountStr);
            if (amount.compareTo(available) > 0) {
                response.setResponseCode(ResponseCode.CARD_LIMIT_EXCEED);
                response.setResponseMessage(ResponseCode.getMessage(ResponseCode.CARD_LIMIT_EXCEED));
                CardLimitValidateResponse exceeded = new CardLimitValidateResponse();
                exceeded.setMaxLimit(max.doubleValue());
                exceeded.setAvailableLimit(available.doubleValue());
                exceeded.setAvailableTranCount(null);
                response.setResponseBody(exceeded);
                return response;
            }
        }

        CardLimitValidateResponse body = new CardLimitValidateResponse();
        body.setMaxLimit(max.doubleValue());
        body.setAvailableLimit(available.doubleValue());
        body.setAvailableTranCount(null);
        response.setResponseCode(ResponseCode.SUCCESS);
        response.setResponseMessage(ResponseCode.getMessage(ResponseCode.SUCCESS));
        response.setResponseBody(body);
        return response;
    }

    /**
     * Read today's remaining from CARD_LIMIT_ACTUAL.
     * Matches authorize keys: CHANNEL_CODE + TRAN_CODE (tran defaults to channel).
     * exactOnly skips the channel-wide fallback so another tran type cannot satisfy this check.
     */
    private BigDecimal findTodayActualAvailable(String pan, Long channelCode, String tranCode, boolean exactOnly) {
        if (pan == null || pan.isBlank() || channelCode == null) {
            return null;
        }
        String channelKey = String.valueOf(channelCode);
        String tranKey = (tranCode != null && !tranCode.isBlank()) ? tranCode.trim() : channelKey;
        LocalDate today = LocalDate.now();

        Optional<CardLimitActual> exact =
                cardLimitActualRepository.findByPanAndChannelCodeAndTranCode(pan.trim(), channelKey, tranKey);
        if (exact.isPresent() && isSameDayCycle(exact.get(), today)) {
            return exact.get().getAvailableLimit();
        }
        if (exactOnly) {
            return null;
        }

        List<CardLimitActual> byChannel =
                cardLimitActualRepository.findByPanAndChannelCode(pan.trim(), channelKey);
        for (CardLimitActual row : byChannel) {
            if (isSameDayCycle(row, today) && row.getAvailableLimit() != null) {
                return row.getAvailableLimit();
            }
        }
        return null;
    }

    private static boolean isSameDayCycle(CardLimitActual row, LocalDate today) {
        return row != null && row.getCycleBeginDate() != null && row.getCycleBeginDate().equals(today);
    }

    private static final String CHANNEL_CODE_REQUIRED_MESSAGE =
            "channelCode must be 1 (ATM), 2 (POS), or 3 (e-commerce)";

    private static boolean isKnownChannelCode(Long channelCode) {
        return channelCode != null && channelCode >= 1L && channelCode <= 3L;
    }

    /** Channel mapping: 1=ATM, 2=POS, 3=e-commerce. No fallback between channels. */
    private BigDecimal pickChannelLimit(LimitProfile profile, Long channelCode) {
        if (channelCode == null) {
            return null;
        }
        if (channelCode == 1L) {
            return profile.getAtmDailyAmount();
        }
        if (channelCode == 2L) {
            return profile.getPosDailyAmount();
        }
        if (channelCode == 3L) {
            return profile.getEcommerceDailyAmount();
        }
        return null;
    }
}
