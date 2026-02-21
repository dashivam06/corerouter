package com.fleebug.corerouter.security.encryption;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageEncryption {

    @Value("${encryption.key}")
    private String encryptionKeyStr;

    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;

    /**
     * Encrypt a message using AES-256
     * 
     * @param message Plain text message to encrypt
     * @return Base64 encoded encrypted message
     */
    public String encrypt(String message) {
        try {
            SecretKey secretKey = getSecretKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(message.getBytes());
            String encryptedMessage = Base64.getEncoder().encodeToString(encryptedBytes);
            
            log.debug("Message encrypted successfully");
            return encryptedMessage;
        } catch (Exception e) {
            log.error("Failed to encrypt message", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt an encrypted message using AES-256
     * 
     * @param encryptedMessage Base64 encoded encrypted message
     * @return Plain text message
     */
    public String decrypt(String encryptedMessage) {
        try {
            SecretKey secretKey = getSecretKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedMessage);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            
            String decryptedMessage = new String(decryptedBytes);
            log.debug("Message decrypted successfully");
            return decryptedMessage;
        } catch (Exception e) {
            log.error("Failed to decrypt message", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Generate or load the secret key
     */
    private SecretKey getSecretKey() {
        try {
            // Decode the base64 encoded key from properties
            byte[] decodedKey = Base64.getDecoder().decode(encryptionKeyStr);
            return new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
        } catch (IllegalArgumentException e) {
            log.error("Invalid encryption key format. Key must be base64 encoded.", e);
            throw new RuntimeException("Invalid encryption key configuration", e);
        }
    }

    /**
     * Generate a new encryption key when needed in case of key rotation.
     * This can be used to create a new key and update the configuration.
     */
    public static String generateNewEncryptionKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_SIZE);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }
}
