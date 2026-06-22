package com.database_migrator.domain.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility class for encrypting and decrypting sensitive data like connector passwords.
 * Uses AES encryption algorithm for reversible encryption.
 */
@Slf4j
@Component
public class EncryptionUtils {

    private static final String ALGORITHM = "AES";

    @Value("${encryption.secret.key:MySecretKey12345}")  // Default key for development
    private String secretKey;

    /**
     * Encrypts a plain text password using AES encryption
     *
     * @param plainText The password to encrypt
     * @return Base64 encoded encrypted password
     */
    public String encrypt(String plainText) {
        try {
            SecretKeySpec key = generateKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);

        } catch (Exception e) {
            log.error("Error encrypting password", e);
            throw new RuntimeException("Failed to encrypt password", e);
        }
    }

    /**
     * Decrypts an encrypted password back to plain text
     *
     * @param encryptedText Base64 encoded encrypted password
     * @return Decrypted plain text password
     */
    public String decrypt(String encryptedText) {
        try {
            SecretKeySpec key = generateKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Error decrypting password", e);
            throw new RuntimeException("Failed to decrypt password", e);
        }
    }

    /**
     * Generates a SecretKeySpec from the configured secret key
     * Pads or truncates the key to 16 bytes (128-bit AES)
     */
    private SecretKeySpec generateKey() {
        try {
            byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            byte[] paddedKey = new byte[16];  // AES-128 requires 16 bytes

            // Pad or truncate to 16 bytes
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, paddedKey.length));

            return new SecretKeySpec(paddedKey, ALGORITHM);
        } catch (Exception e) {
            log.error("Error generating encryption key", e);
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }
}
