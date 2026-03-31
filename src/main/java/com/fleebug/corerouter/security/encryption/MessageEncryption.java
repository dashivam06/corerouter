package com.fleebug.corerouter.security.encryption;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MessageEncryption {

    private final TelemetryClient telemetryClient;

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
            
            // telemetryClient.trackTrace("Message encrypted successfully", SeverityLevel.Verbose, null);
            return encryptedMessage;
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("error", e.getMessage());
            telemetryClient.trackException(e, properties, null);
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
            // telemetryClient.trackTrace("Message decrypted successfully", SeverityLevel.Verbose, null);
            return decryptedMessage;
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("error", e.getMessage());
            telemetryClient.trackException(e, properties, null);
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
            Map<String, String> properties = new HashMap<>();
            properties.put("error", e.getMessage());
            telemetryClient.trackException(e, properties, null);
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
