package com.cms.app.service;

import com.cms.app.config.NetworkKeyProperties;
import com.cms.app.security.PinBlockCipher;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Loads the APS keys once. ZMK is the XOR of the three components and is not
 * applied to a transaction. The selected PVK opens the incoming PIN block.
 */
@Service
public class NetworkKeyService {

    private static final Logger log = LoggerFactory.getLogger(NetworkKeyService.class);

    private final NetworkKeyProperties properties;

    private byte[] acquirerPvk;
    private byte[] issuerPvk;
    private byte[] zmk;
    private String pinBlockSide = "issuer";
    private boolean ready;

    public NetworkKeyService(NetworkKeyProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void load() {
        try {
            acquirerPvk = PinBlockCipher.parseDoubleLengthKey(hex(properties.getAwk().getAcq()));
            issuerPvk = PinBlockCipher.parseDoubleLengthKey(hex(properties.getAwk().getIsr()));
            byte[] c1 = PinBlockCipher.parseDoubleLengthKey(hex(properties.getComponent1()));
            byte[] c2 = PinBlockCipher.parseDoubleLengthKey(hex(properties.getComponent2()));
            byte[] c3 = PinBlockCipher.parseDoubleLengthKey(hex(properties.getComponent3()));
            zmk = PinBlockCipher.xor16(c1, c2, c3);
            pinBlockSide = normalizeSide(properties.getPinblock().getSide());
            ready = true;
            log.info(
                    "APS PIN keys loaded. pinBlock side={} KCV acq={} isr={} zmk={}",
                    pinBlockSide,
                    PinBlockCipher.kcv(acquirerPvk),
                    PinBlockCipher.kcv(issuerPvk),
                    PinBlockCipher.kcv(zmk));
        } catch (RuntimeException ex) {
            ready = false;
            acquirerPvk = null;
            issuerPvk = null;
            zmk = null;
            log.warn("APS PIN keys not loaded. PIN-block authorize is off. App AES PIN still works. {}",
                    ex.getMessage());
        }
    }

    public boolean isReady() {
        return ready;
    }

    /** PVK for the incoming PIN block. ZMK is not returned here. */
    public byte[] pinBlockKey() {
        if (!ready) {
            throw new IllegalStateException("APS PIN keys are not loaded");
        }
        return "acquirer".equals(pinBlockSide) ? acquirerPvk.clone() : issuerPvk.clone();
    }

    public String pinBlockSide() {
        return pinBlockSide;
    }

    private static String hex(NetworkKeyProperties.HexValue value) {
        if (value == null || value.getHex() == null || value.getHex().isBlank()) {
            throw new IllegalArgumentException("a key value is missing");
        }
        return value.getHex();
    }

    private static String normalizeSide(String side) {
        if (side == null || side.isBlank() || "issuer".equalsIgnoreCase(side.trim())) {
            return "issuer";
        }
        if ("acquirer".equalsIgnoreCase(side.trim())) {
            return "acquirer";
        }
        throw new IllegalArgumentException("pinblock side must be issuer or acquirer");
    }
}
