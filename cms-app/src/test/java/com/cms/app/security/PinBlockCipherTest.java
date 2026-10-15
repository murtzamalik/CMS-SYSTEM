package com.cms.app.security;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinBlockCipherTest {

    @Test
    void format0RoundTripUnderDoubleLengthKey() {
        byte[] key = PinBlockCipher.parseDoubleLengthKey("X00112233445566778899AABBCCDDEEFF");
        String pan = "4321987654321098";
        String pin = "1234";

        byte[] clear = PinBlockCipher.buildClearPinBlock(pin, pan);
        byte[] encrypted = PinBlockCipher.encryptPinBlock(key, clear);
        String hex = toHex(encrypted);

        Optional<String> extracted = PinBlockCipher.extractPin(key, hex, pan);
        assertTrue(extracted.isPresent());
        assertEquals(pin, extracted.get());
    }

    @Test
    void zmkIsXorOfThreeComponents() {
        byte[] a = PinBlockCipher.parseDoubleLengthKey("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        byte[] b = PinBlockCipher.parseDoubleLengthKey("00000000000000000000000000000000");
        byte[] c = PinBlockCipher.parseDoubleLengthKey("0F0F0F0F0F0F0F0F0F0F0F0F0F0F0F0F");

        byte[] zmk = PinBlockCipher.xor16(a, b, c);
        assertEquals("F0F0F0F0F0F0F0F0F0F0F0F0F0F0F0F0", toHex(zmk));
    }

    @Test
    void wrongPanDoesNotYieldThePin() {
        byte[] key = PinBlockCipher.parseDoubleLengthKey("00112233445566778899AABBCCDDEEFF");
        byte[] encrypted = PinBlockCipher.encryptPinBlock(
                key, PinBlockCipher.buildClearPinBlock("1234", "4321987654321098"));

        Optional<String> extracted = PinBlockCipher.extractPin(key, toHex(encrypted), "4111111111111111");
        assertTrue(extracted.isEmpty() || !"1234".equals(extracted.get()));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            sb.append(String.format("%02X", value));
        }
        return sb.toString();
    }
}
