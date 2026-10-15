package com.cms.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Decrypts PAN/CVV stored by portal generation (AES-256-GCM).
 * Same key / hash salt as cms-backend.
 */
@Service
public class CardDataEncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKey key;
    private final String hashSalt;

    public CardDataEncryptionService(
            @Value("${cms.card.encryption-key:}") String encryptionKey,
            @Value("${cms.card.encryption-key-env:CARD_ENCRYPTION_KEY}") String envKeyName,
            @Value("${cms.card.hash-salt:cms-pan-salt}") String hashSalt,
            @Value("${cms.card.encryption-optional:true}") boolean optional) {
        String raw = encryptionKey != null && !encryptionKey.isBlank()
                ? encryptionKey
                : System.getenv(envKeyName);
        if (raw == null || raw.isBlank()) {
            if (optional) {
                raw = "0000000000000000000000000000000000000000000000000000000000000000";
            } else {
                throw new IllegalStateException(
                        "Card encryption key is required. Set cms.card.encryption-key or " + envKeyName);
            }
        }
        byte[] keyBytes = decodeKey(raw.trim());
        if (keyBytes.length != 32) {
            throw new IllegalStateException("Card encryption key must be 256 bits (32 bytes).");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
        this.hashSalt = hashSalt != null ? hashSalt : "";
    }

    private static byte[] decodeKey(String raw) {
        if (raw.length() == 64 && raw.matches("[0-9a-fA-F]+")) {
            byte[] b = new byte[32];
            for (int i = 0; i < 32; i++) {
                b[i] = (byte) Integer.parseInt(raw.substring(i * 2, i * 2 + 2), 16);
            }
            return b;
        }
        try {
            return Base64.getDecoder().decode(raw);
        } catch (IllegalArgumentException e) {
            return raw.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            if (decoded.length < GCM_IV_LENGTH + 16) {
                throw new IllegalArgumentException("Invalid encrypted payload");
            }
            ByteBuffer buf = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buf.get(iv);
            byte[] ciphertext = new byte[buf.remaining()];
            buf.get(ciphertext);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Card data decryption failed", e);
        }
    }

    /** AES-GCM first; fall back to legacy plain Base64. */
    public String decryptSensitiveField(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        try {
            return decrypt(stored);
        } catch (RuntimeException aesFailed) {
            try {
                return new String(Base64.getDecoder().decode(stored), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException legacyFailed) {
                return stored;
            }
        }
    }

    /** Same algorithm as cms-backend: SHA-256(salt + pan) hex. */
    public String panHashForLookup(String pan) {
        if (pan == null || pan.isBlank()) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((hashSalt + pan).getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("PAN hash failed", e);
        }
    }
}
