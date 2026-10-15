package com.cms.service;

import com.cms.dal.entity.CmsApiKey;
import com.cms.dal.repository.CmsApiKeyRepository;
import com.cms.dto.response.ApiKeyResponse;
import com.cms.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApiKeyService {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_REVOKED = "REVOKED";
    private static final String KEY_PREFIX_MARKER = "cms_sk_";

    private final CmsApiKeyRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyService(CmsApiKeyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> listAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Revokes any ACTIVE key, then creates a new ACTIVE key.
     * Returns the full secret once in {@link ApiKeyResponse#getSecret()}.
     */
    @Transactional
    public ApiKeyResponse rotate(String createdBy) {
        LocalDateTime now = LocalDateTime.now();
        repository.revokeAllActive(createdBy, now);

        String secret = generateSecret();
        String hash = sha256(secret);
        String displayPrefix = secret.substring(0, Math.min(16, secret.length()));

        CmsApiKey entity = new CmsApiKey();
        entity.setKeyPrefix(displayPrefix);
        entity.setKeyHash(hash);
        entity.setStatus(STATUS_ACTIVE);
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(now);
        entity = repository.save(entity);

        ApiKeyResponse response = toResponse(entity);
        response.setSecret(secret);
        return response;
    }

    @Transactional
    public ApiKeyResponse revoke(Long id, String revokedBy) {
        CmsApiKey entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found: " + id));
        if (STATUS_REVOKED.equals(entity.getStatus())) {
            return toResponse(entity);
        }
        entity.setStatus(STATUS_REVOKED);
        entity.setRevokedBy(revokedBy);
        entity.setRevokedAt(LocalDateTime.now());
        return toResponse(repository.save(entity));
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return KEY_PREFIX_MARKER + HexFormat.of().formatHex(bytes);
    }

    private ApiKeyResponse toResponse(CmsApiKey entity) {
        ApiKeyResponse r = new ApiKeyResponse();
        r.setId(entity.getId());
        r.setKeyPrefix(entity.getKeyPrefix());
        r.setStatus(entity.getStatus());
        r.setCreatedBy(entity.getCreatedBy());
        r.setCreatedAt(entity.getCreatedAt());
        r.setRevokedBy(entity.getRevokedBy());
        r.setRevokedAt(entity.getRevokedAt());
        r.setLastUsedAt(entity.getLastUsedAt());
        return r;
    }
}
