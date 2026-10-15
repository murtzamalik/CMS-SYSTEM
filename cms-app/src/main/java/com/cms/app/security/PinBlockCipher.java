package com.cms.app.security;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Optional;

/**
 * ISO 9564-1 format-0 PIN block under a double-length 3DES key.
 * The key opens the PIN block only. It is not used to encrypt the APS response.
 */
public final class PinBlockCipher {

    private PinBlockCipher() {
    }

    /** Strip a leading X scheme marker and require 32 hex chars (16 bytes). */
    public static byte[] parseDoubleLengthKey(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("key is empty");
        }
        String hex = raw.trim();
        if (hex.length() > 1 && (hex.charAt(0) == 'X' || hex.charAt(0) == 'x')) {
            hex = hex.substring(1);
        }
        byte[] key = parseHex(hex);
        if (key.length != 16) {
            throw new IllegalArgumentException("PIN key must be 16 bytes");
        }
        return key;
    }

    public static byte[] xor16(byte[] a, byte[] b, byte[] c) {
        if (a.length != 16 || b.length != 16 || c.length != 16) {
            throw new IllegalArgumentException("ZMK components must be 16 bytes");
        }
        byte[] out = new byte[16];
        for (int i = 0; i < 16; i++) {
            out[i] = (byte) (a[i] ^ b[i] ^ c[i]);
        }
        return out;
    }

    /** First 3 bytes of 3DES(zeros), as 6 hex chars. Safe to log; the key is not. */
    public static String kcv(byte[] key16) {
        byte[] encrypted = crypt(key16, new byte[8], Cipher.ENCRYPT_MODE);
        return toHex(encrypted).substring(0, 6);
    }

    public static byte[] encryptPinBlock(byte[] key16, byte[] clearBlock) {
        if (clearBlock.length != 8) {
            throw new IllegalArgumentException("PIN block must be 8 bytes");
        }
        return crypt(key16, clearBlock, Cipher.ENCRYPT_MODE);
    }

    public static byte[] decryptPinBlock(byte[] key16, byte[] encryptedBlock) {
        if (encryptedBlock.length != 8) {
            throw new IllegalArgumentException("PIN block must be 8 bytes");
        }
        return crypt(key16, encryptedBlock, Cipher.DECRYPT_MODE);
    }

    /**
     * Open a hex PIN block and return the PIN digits.
     * Empty when the block, key, or PAN cannot produce a format-0 PIN.
     */
    public static Optional<String> extractPin(byte[] key16, String pinBlockHex, String pan) {
        try {
            byte[] encrypted = parseHex(pinBlockHex.trim());
            if (encrypted.length != 8) {
                return Optional.empty();
            }
            byte[] clear = decryptPinBlock(key16, encrypted);
            byte[] panField = panField(pan);
            for (int i = 0; i < 8; i++) {
                clear[i] ^= panField[i];
            }
            int format = (clear[0] >> 4) & 0x0F;
            int length = clear[0] & 0x0F;
            if (format != 0 || length < 4 || length > 12) {
                return Optional.empty();
            }
            StringBuilder pin = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                int nibble = nibbleAt(clear, i + 2);
                if (nibble > 9) {
                    return Optional.empty();
                }
                pin.append(nibble);
            }
            for (int i = length + 2; i < 16; i++) {
                if (nibbleAt(clear, i) != 0x0F) {
                    return Optional.empty();
                }
            }
            return Optional.of(pin.toString());
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    /** Test helper. Builds the clear format-0 block (PIN field XOR PAN field). */
    public static byte[] buildClearPinBlock(String pin, String pan) {
        if (pin == null || !pin.matches("\\d{4,12}")) {
            throw new IllegalArgumentException("PIN must be 4 to 12 digits");
        }
        byte[] block = new byte[8];
        Arrays.fill(block, (byte) 0xFF);
        block[0] = (byte) (pin.length() & 0x0F);
        for (int i = 0; i < pin.length(); i++) {
            setNibble(block, i + 2, pin.charAt(i) - '0');
        }
        byte[] panField = panField(pan);
        for (int i = 0; i < 8; i++) {
            block[i] ^= panField[i];
        }
        return block;
    }

    static byte[] panField(String pan) {
        if (pan == null) {
            throw new IllegalArgumentException("PAN is required");
        }
        String digits = pan.replaceAll("\\D", "");
        if (digits.length() < 2) {
            throw new IllegalArgumentException("PAN is too short");
        }
        String withoutCheck = digits.substring(0, digits.length() - 1);
        if (withoutCheck.length() > 12) {
            withoutCheck = withoutCheck.substring(withoutCheck.length() - 12);
        }
        while (withoutCheck.length() < 12) {
            withoutCheck = "0" + withoutCheck;
        }
        byte[] field = new byte[8];
        for (int i = 0; i < 12; i++) {
            setNibble(field, i + 4, withoutCheck.charAt(i) - '0');
        }
        return field;
    }

    private static byte[] crypt(byte[] key16, byte[] block, int mode) {
        try {
            byte[] key24 = new byte[24];
            System.arraycopy(key16, 0, key24, 0, 16);
            System.arraycopy(key16, 0, key24, 16, 8);
            Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
            cipher.init(mode, new SecretKeySpec(key24, "DESede"));
            return cipher.doFinal(block);
        } catch (Exception ex) {
            throw new IllegalStateException("PIN block crypto failed");
        }
    }

    static byte[] parseHex(String hex) {
        String clean = hex.trim();
        if (clean.length() % 2 != 0 || !clean.matches("[0-9A-Fa-f]+")) {
            throw new IllegalArgumentException("invalid hex");
        }
        byte[] out = new byte[clean.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private static int nibbleAt(byte[] data, int index) {
        int shift = (index % 2 == 0) ? 4 : 0;
        return (data[index / 2] >> shift) & 0x0F;
    }

    private static void setNibble(byte[] data, int index, int nibble) {
        int byteIndex = index / 2;
        if (index % 2 == 0) {
            data[byteIndex] = (byte) ((data[byteIndex] & 0x0F) | ((nibble & 0x0F) << 4));
        } else {
            data[byteIndex] = (byte) ((data[byteIndex] & 0xF0) | (nibble & 0x0F));
        }
    }
}
