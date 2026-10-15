package com.cms.app.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpProofServiceTest {

    @Test
    void opaqueProofAcceptedWhenSecretEmpty() {
        OtpProofService svc = new OtpProofService("", 8, 300);
        assertNull(svc.validateForgotPinProof("uat-otp-ref-001", "123", "4111"));
        assertNotNull(svc.validateForgotPinProof("short", "123", "4111"));
        assertNotNull(svc.validateForgotPinProof("  ", "123", "4111"));
    }

    @Test
    void signedProofRoundTrip() {
        OtpProofService svc = new OtpProofService("test-otp-secret", 8, 600);
        String proof = svc.createForgotPinProof("REL123", "4111111111111111", 300);
        assertTrue(proof.contains("."));
        assertNull(svc.validateForgotPinProof(proof, "REL123", "4111111111111111"));
        assertNotNull(svc.validateForgotPinProof(proof, "OTHER", "4111111111111111"));
    }
}
