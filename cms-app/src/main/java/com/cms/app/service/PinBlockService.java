package com.cms.app.service;

import com.cms.app.config.AESencryption;
import com.cms.app.security.PinBlockCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

/**
 * Checks an APS PIN block against the PIN already stored for the mobile app.
 * The clear PIN is not logged and is not written back onto the card.
 */
@Service
public class PinBlockService {

    private static final Logger log = LoggerFactory.getLogger(PinBlockService.class);

    private final NetworkKeyService networkKeyService;
    private final AESencryption aesEncryption;

    public PinBlockService(NetworkKeyService networkKeyService, AESencryption aesEncryption) {
        this.networkKeyService = networkKeyService;
        this.aesEncryption = aesEncryption;
    }

    public boolean isReady() {
        return networkKeyService.isReady();
    }

    public boolean matches(String pinBlockHex, String pan, String storedPin) {
        if (!networkKeyService.isReady()) {
            return false;
        }
        Optional<String> fromBlock = PinBlockCipher.extractPin(networkKeyService.pinBlockKey(), pinBlockHex, pan);
        if (fromBlock.isEmpty()) {
            log.warn("PIN block could not be read");
            return false;
        }
        String storedClear = aesEncryption.decrypt(storedPin);
        if (storedClear == null || !constantTimeEquals(storedClear, fromBlock.get())) {
            log.warn("PIN block did not match the stored PIN");
            return false;
        }
        return true;
    }

    private static boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8));
    }
}
