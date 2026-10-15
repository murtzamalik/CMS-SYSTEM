package com.cms.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Validates LasPay OTP proof on forgot-PIN ({@code flag=F}).
 * CMS does not send or check SMS OTP; LasPay verifies OTP then sends {@code otpProof}.
 *
 * <p>Signed format (when {@code cms.app.otp-proof.secret} is set):
 * {@code base64url(jsonPayload) + "." + base64url(hmacSha256(secret, jsonPayload))}
 * Payload JSON fields: {@code relationshipNum}, {@code pan}, {@code exp} (unix seconds),
 * {@code purpose} must be {@code FORGOT_PIN}.
 *
 * <p>When secret is empty: any non-blank proof (min length) is accepted as an opaque {@code otpRef}
 * for UAT. Production should always set the shared secret.
 */
@Service
public class OtpProofService {

    public static final String PURPOSE_FORGOT_PIN = "FORGOT_PIN";

    private final String secret;
    private final int minOpaqueLength;
    private final long maxSkewSeconds;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OtpProofService(
            @Value("${cms.app.otp-proof.secret:}") String secret,
            @Value("${cms.app.otp-proof.min-opaque-length:8}") int minOpaqueLength,
            @Value("${cms.app.otp-proof.max-skew-seconds:300}") long maxSkewSeconds) {
        this.secret = secret != null ? secret.trim() : "";
        this.minOpaqueLength = Math.max(1, minOpaqueLength);
        this.maxSkewSeconds = Math.max(60, maxSkewSeconds);
    }

    public boolean isSignatureEnforced() {
        return !secret.isBlank();
    }

    /**
     * @return null if valid; otherwise a short error reason for the API message
     */
    public String validateForgotPinProof(String otpProof, String relationshipNum, String pan) {
        if (otpProof == null || otpProof.isBlank()) {
            return "otpProof is required after LasPay OTP for forgot-PIN";
        }
        String proof = otpProof.trim();
        if (!secret.isBlank()) {
            return validateSigned(proof, relationshipNum, pan);
        }
        if (proof.length() < minOpaqueLength) {
            return "otpProof is too short";
        }
        return null;
    }

    private String validateSigned(String proof, String relationshipNum, String pan) {
        int dot = proof.indexOf('.');
        if (dot <= 0 || dot == proof.length() - 1) {
            return "otpProof must be payload.signature";
        }
        String payloadB64 = proof.substring(0, dot);
        String sigB64 = proof.substring(dot + 1);
        byte[] payloadBytes;
        try {
            payloadBytes = Base64.getUrlDecoder().decode(payloadB64);
        } catch (IllegalArgumentException ex) {
            return "otpProof payload is not valid base64url";
        }
        String expectedSig = hmacBase64Url(payloadBytes);
        if (!constantTimeEquals(expectedSig, sigB64)) {
            return "otpProof signature is invalid";
        }
        try {
            JsonNode node = objectMapper.readTree(payloadBytes);
            String purpose = text(node, "purpose");
            if (purpose == null || !PURPOSE_FORGOT_PIN.equalsIgnoreCase(purpose.trim())) {
                return "otpProof purpose must be FORGOT_PIN";
            }
            String rel = text(node, "relationshipNum");
            if (relationshipNum != null && !relationshipNum.isBlank()
                    && (rel == null || !relationshipNum.trim().equals(rel.trim()))) {
                return "otpProof relationshipNum does not match";
            }
            String proofPan = text(node, "pan");
            if (pan != null && !pan.isBlank() && proofPan != null && !proofPan.isBlank()
                    && !pansCompatible(pan.trim(), proofPan.trim())) {
                return "otpProof pan does not match";
            }
            long exp = node.path("exp").asLong(0L);
            long now = Instant.now().getEpochSecond();
            if (exp <= 0) {
                return "otpProof exp is required";
            }
            if (exp < now) {
                return "otpProof has expired";
            }
            if (exp > now + maxSkewSeconds) {
                return "otpProof exp is too far in the future";
            }
            return null;
        } catch (Exception ex) {
            return "otpProof payload is not valid JSON";
        }
    }

    private static boolean pansCompatible(String requestPan, String proofPan) {
        if (requestPan.equals(proofPan)) {
            return true;
        }
        String a = digitsOnly(requestPan);
        String b = digitsOnly(proofPan);
        if (!a.isEmpty() && a.equals(b)) {
            return true;
        }
        // Masked vs full: compare last 4 when both have them
        if (a.length() >= 4 && b.length() >= 4) {
            return a.substring(a.length() - 4).equals(b.substring(b.length() - 4));
        }
        return false;
    }

    private static String digitsOnly(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String hmacBase64Url(byte[] payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failed", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < x.length; i++) {
            r |= x[i] ^ y[i];
        }
        return r == 0;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    /** Test/helper: build a signed proof LasPay-style (same secret). */
    public String createForgotPinProof(String relationshipNum, String pan, long ttlSeconds) {
        if (secret.isBlank()) {
            throw new IllegalStateException("cms.app.otp-proof.secret is not configured");
        }
        try {
            long exp = Instant.now().getEpochSecond() + Math.max(60, ttlSeconds);
            String json = objectMapper.createObjectNode()
                    .put("purpose", PURPOSE_FORGOT_PIN)
                    .put("relationshipNum", relationshipNum)
                    .put("pan", pan)
                    .put("exp", exp)
                    .toString();
            byte[] payload = json.getBytes(StandardCharsets.UTF_8);
            String payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
            return payloadB64 + "." + hmacBase64Url(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create otpProof", e);
        }
    }
}
