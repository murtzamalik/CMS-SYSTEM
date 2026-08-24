package com.cms.service.impl;

import com.cms.dal.entity.CardType;
import com.cms.dal.repository.CardProductRepository;
import com.cms.dal.repository.CardTypeRepository;
import com.cms.dal.repository.LimitProfileRepository;
import com.cms.dto.request.CardTypeCreateRequest;
import com.cms.dto.request.CardTypeUpdateRequest;
import com.cms.dto.response.CardTypeResponse;
import com.cms.exception.DuplicateResourceException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.mapper.CardTypeMapper;
import com.cms.service.CardTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CardTypeServiceImpl implements CardTypeService {

    private final CardTypeRepository cardTypeRepository;
    private final CardProductRepository cardProductRepository;
    private final LimitProfileRepository limitProfileRepository;
    private final CardTypeMapper cardTypeMapper;

    public CardTypeServiceImpl(CardTypeRepository cardTypeRepository, CardProductRepository cardProductRepository,
                               LimitProfileRepository limitProfileRepository, CardTypeMapper cardTypeMapper) {
        this.cardTypeRepository = cardTypeRepository;
        this.cardProductRepository = cardProductRepository;
        this.limitProfileRepository = limitProfileRepository;
        this.cardTypeMapper = cardTypeMapper;
    }

    @Override
    @Transactional
    public CardTypeResponse create(CardTypeCreateRequest request) {
        if (request.getCardTypeCode() != null && cardTypeRepository.findByCardTypeCode(request.getCardTypeCode()).isPresent())
            throw new DuplicateResourceException("CardType", request.getCardTypeCode());
        if (request.getProductId() != null) {
            cardProductRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("CardProduct", String.valueOf(request.getProductId())));
        } else if (request.getProductCode() != null && !request.getProductCode().isBlank()) {
            if (!cardProductRepository.findByProductCode(request.getProductCode()).isPresent())
                throw new ResourceNotFoundException("CardProduct", request.getProductCode());
        } else {
            throw new com.cms.exception.BusinessValidationException("Either productId or productCode is required");
        }
        validateLimitProfile(request.getDefaultLimitProfileId());
        CardType e = cardTypeMapper.toEntity(request);
        // Map card type code to numeric before saving
        e.setCardTypeCode(mapCardTypeCode(request.getCardTypeCode()));
        if (request.getProductId() != null) {
            cardProductRepository.findById(request.getProductId())
                .ifPresent(p -> { e.setProductId(p.getId()); e.setProductCode(p.getProductCode()); });
        } else if (request.getProductCode() != null)
            cardProductRepository.findByProductCode(request.getProductCode()).ifPresent(p -> { e.setProductId(p.getId()); e.setProductCode(p.getProductCode()); });
        e.setCreatedOn(LocalDateTime.now());
        e.setUpdatedOn(LocalDateTime.now());
        return cardTypeMapper.toResponse(cardTypeRepository.save(e));
    }

    // Maps card type code name to numeric code for DB storage
    private String mapCardTypeCode(String code) {
        if (code == null) return null;
        return switch (code.toUpperCase().trim()) {
            case "DEBIT_STD", "DEBIT" -> "005";
            case "CREDIT_STD", "CREDIT" -> "006";
            case "TEST" -> "007";
            default -> code;
        };
    }

    private void validateLimitProfile(Long limitProfileId) {
        if (limitProfileId == null) return;
        if (!limitProfileRepository.existsById(limitProfileId)) {
            throw new ResourceNotFoundException("LimitProfile", String.valueOf(limitProfileId));
        }
    }

    @Override
    public List<CardTypeResponse> findAll() {
        return cardTypeMapper.toResponseList(cardTypeRepository.findAllByOrderByCardTypeCode());
    }

    @Override
    public CardTypeResponse getById(Long id) {
        CardType e = cardTypeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("CardType", String.valueOf(id)));
        return cardTypeMapper.toResponse(e);
    }

    @Override
    @Transactional
    public CardTypeResponse update(Long id, CardTypeUpdateRequest request) {
        CardType e = cardTypeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("CardType", String.valueOf(id)));
        if (request.getProductId() != null) {
            cardProductRepository.findById(request.getProductId())
                .ifPresent(p -> { e.setProductCode(p.getProductCode()); e.setProductId(p.getId()); });
        } else if (request.getProductCode() != null) {
            cardProductRepository.findByProductCode(request.getProductCode())
                .ifPresent(p -> { e.setProductCode(p.getProductCode()); e.setProductId(p.getId()); });
        }
        if (request.getCardTypeName() != null) e.setCardTypeName(request.getCardTypeName());
        if (request.getIsActive() != null) e.setIsActive(Boolean.TRUE.equals(request.getIsActive()) ? 1 : 0);
        if (request.getSupplementaryAllowed() != null) e.setSupplementaryAllowed(request.getSupplementaryAllowed());
        if (request.getIsSuppType() != null) e.setIsSuppType(request.getIsSuppType());
        if (request.getSuppTypeCode() != null) e.setSuppTypeCode(request.getSuppTypeCode());
        if (request.getPanLength() != null) e.setPanLength(request.getPanLength());
        if (request.getBin() != null) e.setBin(request.getBin());
        if (request.getDefaultLimitProfileId() != null) {
            validateLimitProfile(request.getDefaultLimitProfileId());
            e.setDefaultLimitProfileId(request.getDefaultLimitProfileId());
        }
        if (request.getExpPeriod() != null) e.setExpPeriod(request.getExpPeriod());
        if (request.getPanSequenceName() != null) e.setPanSequenceName(request.getPanSequenceName());
        if (request.getPanSequenceLength() != null) e.setPanSequenceLength(request.getPanSequenceLength());
        e.setUpdatedOn(LocalDateTime.now());
        return cardTypeMapper.toResponse(cardTypeRepository.save(e));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!cardTypeRepository.existsById(id))
            throw new ResourceNotFoundException("CardType", String.valueOf(id));
        cardTypeRepository.deleteById(id);
    }
}
