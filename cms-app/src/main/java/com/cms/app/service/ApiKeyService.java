package com.cms.app.service;

import com.cms.app.repository.CmsApiKeyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ApiKeyService {

    public static final String STATUS_ACTIVE = "ACTIVE";

    private final CmsApiKeyRepository repository;
    private final Set<String> bootstrapKeyHashes;

    public ApiKeyService(
            CmsApiKeyRepository repository,
            @Value("${cms.app.api-keys:}") String bootstrapKeys) {
        this.repository = repository;
        this.bootstrapKeyHashes = Arrays.stream(bootstrapKeys.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(ApiKeyService::sha256)
                .collect(Collectors.toSet());
    }

    public boolean isValidActiveKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return false;
        }
        String hash = sha256(rawKey.trim());
        if (bootstrapKeyHashes.contains(hash)) {
            return true;
        }
        return repository.findByKeyHashAndStatus(hash, STATUS_ACTIVE).isPresent();
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
}
