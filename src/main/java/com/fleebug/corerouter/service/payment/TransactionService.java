package com.fleebug.corerouter.service.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleebug.corerouter.entity.payment.Transaction;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.payment.TransactionStatus;
import com.fleebug.corerouter.enums.payment.TransactionType;
import com.fleebug.corerouter.exception.payment.TransactionNotFoundException;
import com.fleebug.corerouter.exception.payment.TransactionVerificationException;
import com.fleebug.corerouter.exception.payment.InvalidTransactionAmountException;
import com.fleebug.corerouter.repository.payment.TransactionRepository;
import com.fleebug.corerouter.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${esewa.merchant.id}")
    private String merchantId;

    @Value("${esewa.secret.key}")
    private String secretKey;

    @Value("${esewa.payment.url}")
    private String paymentUrl;

    @Value("${esewa.verify.url}")
    private String verifyUrl;

    @Value("${esewa.success.url}")
    private String successUrl;

    @Value("${esewa.failure.url}")
    private String failureUrl;

    @Transactional
    public Map<String, String> initiateTopUp(User user, BigDecimal amount) {
        String transactionUuid = UUID.randomUUID().toString();

        // Create Pending Transaction
        Transaction transaction = Transaction.builder()
                .user(user)
                .amount(amount)
                .esewaTransactionId(transactionUuid)
                .type(TransactionType.WALLET_TOPUP)
                .status(TransactionStatus.PENDING)
                .productCode(merchantId)
                .createdAt(LocalDateTime.now())
                .build();
        
        transactionRepository.save(transaction);

        // Generate Signature
        String signature = generateSignature(amount, transactionUuid, merchantId);

        // Prepare Form Data
        Map<String, String> paymentData = new HashMap<>();
        paymentData.put("amount", amount.toString());
        paymentData.put("tax_amount", "0");
        paymentData.put("total_amount", amount.toString());
        paymentData.put("transaction_uuid", transactionUuid);
        paymentData.put("product_code", merchantId);
        paymentData.put("product_service_charge", "0");
        paymentData.put("product_delivery_charge", "0");
        paymentData.put("success_url", successUrl);
        paymentData.put("failure_url", failureUrl);
        paymentData.put("signed_field_names", "total_amount,transaction_uuid,product_code");
        paymentData.put("signature", signature);
        paymentData.put("payment_url", paymentUrl);

        return paymentData;
    }

    @Transactional
    public Transaction verifyAndCompleteTransaction(String encodedData) {
        try {
            // Decode Base64 data
            byte[] decodedBytes = Base64.getDecoder().decode(encodedData);
            String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
            
           
            JsonNode jsonNode = objectMapper.readTree(decodedString);
            String transactionUuid = jsonNode.get("transaction_uuid").asText();
            String totalAmount = jsonNode.get("total_amount").asText().replace(",", ""); // Handle commas if any
            String status = jsonNode.get("status").asText();

            if (!"COMPLETE".equals(status)) {
                log.warn("Transaction status not COMPLETE: {}", status);
                throw new TransactionVerificationException("Transaction status is " + status);
            }

            // Find Transaction
            Transaction transaction = transactionRepository.findByEsewaTransactionId(transactionUuid)
                    .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + transactionUuid));

            if (transaction.getStatus() == TransactionStatus.COMPLETED) {
                log.info("Transaction already completed: {}", transactionUuid);
                return transaction;
            }

            // Verify with eSewa API
            String verificationUrl = String.format("%s?product_code=%s&total_amount=%s&transaction_uuid=%s",
                    verifyUrl, merchantId, totalAmount, transactionUuid);

            log.info("Verifying transaction with URL: {}", verificationUrl);
            
            try {
                // eSewa status check returns JSON
                @SuppressWarnings("unchecked")
                Map<String, Object> response = restTemplate.getForObject(verificationUrl, Map.class);
                String verifiedStatus = (String) response.get("status");
                
                if ("COMPLETE".equals(verifiedStatus)) {
                    // Update Transaction
                    transaction.setStatus(TransactionStatus.COMPLETED);
                    transaction.setCompletedAt(LocalDateTime.now());
                    transactionRepository.save(transaction);

                    // Credit Wallet
                    User user = transaction.getUser();
                    user.setBalance(user.getBalance().add(transaction.getAmount()));
                    userRepository.save(user);
                    
                    log.info("Wallet credited for user: {}", user.getUserId());
                    return transaction;
                } else {
                    log.error("eSewa verification failed. Status: {}", verifiedStatus);
                    transaction.setStatus(TransactionStatus.FAILED);
                    transactionRepository.save(transaction);
                    throw new TransactionVerificationException("eSewa verification failed with status: " + verifiedStatus);
                }
            } catch (Exception e) {
                log.error("Error calling eSewa verification API", e);
                // IF we already threw an exception, rethrow it. Otherwise wrap it.
                if (e instanceof TransactionVerificationException) {
                    throw e;
                }
                throw new TransactionVerificationException("Error calling eSewa verification API", e);
            }

        } catch (JsonProcessingException e) {
            log.error("Error parsing eSewa callback data", e);
            throw new TransactionVerificationException("Invalid callback data", e);
        } catch (RuntimeException e) {
             throw e; // Rethrow runtime exceptions (like TransactionNotFoundException)
        } catch (Exception e) {
             log.error("Error processing transaction success", e);
             throw new TransactionVerificationException("Error processing transaction success", e);
        }
    }

    @Transactional
    public void markTransactionFailed(String encodedData, String fallbackUuid) {
        log.info("Processing failure callback. fallbackUuid: {}", fallbackUuid);
        String transactionUuid = null;
        try {
            if (encodedData != null && !encodedData.isEmpty()) {
                // Attempt standard Base64
                byte[] decodedBytes = Base64.getDecoder().decode(encodedData);
                String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
                log.debug("Decoded failure data successfully");
                
                JsonNode jsonNode = objectMapper.readTree(decodedString);
                transactionUuid = jsonNode.path("transaction_uuid").asText();
            }
        } catch (IllegalArgumentException e) {
            log.warn("Standard Base64 decoding failed, trying URL decoder");
            try {
                byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedData);
                String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
                JsonNode jsonNode = objectMapper.readTree(decodedString);
                transactionUuid = jsonNode.path("transaction_uuid").asText();
            } catch (Exception ex) {
                log.error("Failed to decode failure data with URL decoder", ex);
            }
        } catch (Exception e) {
            log.error("Error parsing failure callback JSON", e);
        }

        if (transactionUuid == null || transactionUuid.isEmpty()) {
            transactionUuid = fallbackUuid;
        }

        if (transactionUuid != null && !transactionUuid.isEmpty()) {
            final String finalUuid = transactionUuid;
            transactionRepository.findByEsewaTransactionId(finalUuid)
                    .ifPresentOrElse(transaction -> {
                        transaction.setStatus(TransactionStatus.FAILED);
                        transactionRepository.save(transaction);
                        log.info("Marked transaction {} as FAILED", finalUuid);
                    }, () -> log.warn("Transaction not found for failure callback: {}", finalUuid));
        } else {
            log.warn("Could not extract transaction UUID from failure callback");
        }
    }

    private String generateSignature(BigDecimal amount, String transactionUuid, String merchantId) {
        try {
            String totalAmount = amount.toString(); // Ensure format matches exactly what is sent
            // Message format: total_amount,transaction_uuid,product_code
            String message = "total_amount=" + totalAmount + ",transaction_uuid=" + transactionUuid + ",product_code=" + merchantId;
            
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            
            byte[] hash = sha256_HMAC.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error generating signature", e);
        }
    }

    @Scheduled(fixedDelay = 60000) // Run every minute
    @Transactional
    public void expirePendingTransactions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        List<Transaction> pendingTransactions = transactionRepository.findByStatusAndCreatedAtBefore(TransactionStatus.PENDING, cutoff);
        
        if (!pendingTransactions.isEmpty()) {
            log.info("Found {} pending transactions older than 30 minutes. Marking as FAILED.", pendingTransactions.size());
            for (Transaction t : pendingTransactions) {
                t.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(t);
            }
        }
    }
}