package com.fleebug.corerouter.service.payment;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleebug.corerouter.entity.payment.Transaction;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.payment.TransactionStatus;
import com.fleebug.corerouter.enums.payment.TransactionType;
import com.fleebug.corerouter.exception.payment.TransactionNotFoundException;
import com.fleebug.corerouter.exception.payment.TransactionVerificationException;
import com.fleebug.corerouter.repository.payment.TransactionRepository;
import com.fleebug.corerouter.repository.user.UserRepository;
import com.fleebug.corerouter.util.HttpClientUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class TransactionService {

    private final TelemetryClient telemetryClient;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final HttpClientUtil httpClientUtil;
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
                telemetryClient.trackTrace("Transaction status not COMPLETE: " + status, SeverityLevel.Information, Map.of("status", status));
                throw new TransactionVerificationException("Transaction status is " + status);
            }

            // Find Transaction
            Transaction transaction = transactionRepository.findByEsewaTransactionId(transactionUuid)
                    .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + transactionUuid));

            if (transaction.getStatus() == TransactionStatus.COMPLETED) {
                telemetryClient.trackTrace("Transaction already completed: " + transactionUuid, SeverityLevel.Information, Map.of("transactionUuid", transactionUuid));
                return transaction;
            }

            // Verify with eSewa API
            String verificationUrl = String.format("%s?product_code=%s&total_amount=%s&transaction_uuid=%s",
                    verifyUrl, merchantId, totalAmount, transactionUuid);

            telemetryClient.trackTrace("Verifying transaction with URL: " + verificationUrl, SeverityLevel.Information, Map.of("verificationUrl", verificationUrl));
            
            try {
                // eSewa status check returns JSON
                Map<String, Object> response = httpClientUtil.getJsonMap(verificationUrl, Map.of(), 5000, 10000);
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
                    
                    telemetryClient.trackTrace("Wallet credited for user: " + user.getUserId(), SeverityLevel.Information, Map.of("userId", String.valueOf(user.getUserId())));
                    return transaction;
                } else {
                    telemetryClient.trackTrace("eSewa verification failed. Status: " + verifiedStatus, SeverityLevel.Warning, Map.of("verifiedStatus", verifiedStatus != null ? verifiedStatus : "null"));
                    transaction.setStatus(TransactionStatus.FAILED);
                    transactionRepository.save(transaction);
                    throw new TransactionVerificationException("eSewa verification failed with status: " + verifiedStatus);
                }
            } catch (Exception e) {
                telemetryClient.trackException(e, Map.of("error", "Error calling eSewa verification API"), null);
                // IF we already threw an exception, rethrow it. Otherwise wrap it.
                if (e instanceof TransactionVerificationException) {
                    throw e;
                }
                throw new TransactionVerificationException("Error calling eSewa verification API", e);
            }

        } catch (JsonProcessingException e) {
            telemetryClient.trackException(e, Map.of("error", "Error parsing eSewa callback data"), null);
            throw new TransactionVerificationException("Invalid callback data", e);
        } catch (RuntimeException e) {
             throw e; // Rethrow runtime exceptions (like TransactionNotFoundException)
        } catch (Exception e) {
             telemetryClient.trackException(e, Map.of("error", "Error processing transaction success"), null);
             throw new TransactionVerificationException("Error processing transaction success", e);
        }
    }

    @Transactional
    public void markTransactionFailed(String encodedData, String fallbackUuid) {
        telemetryClient.trackTrace("Processing failure callback. fallbackUuid: " + fallbackUuid, SeverityLevel.Information, Map.of("fallbackUuid", fallbackUuid != null ? fallbackUuid : "null"));
        String transactionUuid = null;
        try {
            if (encodedData != null && !encodedData.isEmpty()) {
                // Attempt standard Base64
                byte[] decodedBytes = Base64.getDecoder().decode(encodedData);
                String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
                // telemetryClient.trackTrace("Decoded failure data successfully", SeverityLevel.Verbose, null);
                
                JsonNode jsonNode = objectMapper.readTree(decodedString);
                transactionUuid = jsonNode.path("transaction_uuid").asText();
            }
        } catch (IllegalArgumentException e) {
            telemetryClient.trackTrace("Standard Base64 decoding failed, trying URL decoder", SeverityLevel.Information, null);
            try {
                byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedData);
                String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
                JsonNode jsonNode = objectMapper.readTree(decodedString);
                transactionUuid = jsonNode.path("transaction_uuid").asText();
            } catch (Exception ex) {
                telemetryClient.trackException(ex, Map.of("error", "Failed to decode failure data with URL decoder"), null);
            }
        } catch (Exception e) {
            telemetryClient.trackException(e, Map.of("error", "Error parsing failure callback JSON"), null);
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
                        telemetryClient.trackTrace("Marked transaction " + finalUuid + " as FAILED", SeverityLevel.Information, Map.of("transactionUuid", finalUuid));
                    }, () -> telemetryClient.trackTrace("Transaction not found for failure callback: " + finalUuid, SeverityLevel.Information, Map.of("transactionUuid", finalUuid)));
        } else {
            telemetryClient.trackTrace("Could not extract transaction UUID from failure callback", SeverityLevel.Information, null);
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
            telemetryClient.trackTrace("Found " + pendingTransactions.size() + " pending transactions older than 30 minutes. Marking as FAILED.", SeverityLevel.Information, Map.of("count", String.valueOf(pendingTransactions.size())));
            for (Transaction t : pendingTransactions) {
                t.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(t);
            }
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal getTopUpAmountByUserAndPeriod(Integer userId, LocalDateTime from, LocalDateTime to) {
        return transactionRepository.sumAmountByUserAndTypeAndStatusAndCompletedAtBetween(
                userId,
                TransactionType.WALLET_TOPUP,
                TransactionStatus.COMPLETED,
                from,
                to
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal getTopUpAmountByPeriod(LocalDateTime from, LocalDateTime to) {
        return transactionRepository.sumAmountByTypeAndStatusAndCompletedAtBetween(
                TransactionType.WALLET_TOPUP,
                TransactionStatus.COMPLETED,
                from,
                to
        );
    }

    /**
     * Get list of transactions filtered by type, status and date range
     */
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByFilters(
            TransactionType type,
            TransactionStatus status,
            LocalDateTime from,
            LocalDateTime to) {
        if (type != null && status != null) {
            return transactionRepository.findByTypeAndStatusAndCompletedAtBetween(
                    type, status, from, to,
                    org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)
            ).getContent();
        } else if (type != null) {
            // If only type is specified, get all by type
            List<Transaction> allByType = transactionRepository.findByType(type);
            return allByType.stream()
                    .filter(t -> t.getCompletedAt() != null && 
                               t.getCompletedAt().isAfter(from) && 
                               t.getCompletedAt().isBefore(to))
                    .sorted((a, b) -> b.getCompletedAt().compareTo(a.getCompletedAt()))
                    .toList();
        } else if (status != null) {
            // If only status is specified, get all by status
            List<Transaction> allByStatus = transactionRepository.findByStatus(status);
            return allByStatus.stream()
                    .filter(t -> t.getCompletedAt() != null && 
                               t.getCompletedAt().isAfter(from) && 
                               t.getCompletedAt().isBefore(to))
                    .sorted((a, b) -> b.getCompletedAt().compareTo(a.getCompletedAt()))
                    .toList();
        } else {
            // Get all transactions in date range
            return transactionRepository.findByCompletedAtBetween(from, to);
        }
    }

    /**
     * Get daily earnings (sum of completed WALLET_TOPUP transactions grouped by date)
     */
    @Transactional(readOnly = true)
    public List<Object[]> getDailyEarnings(LocalDateTime from, LocalDateTime to) {
        return transactionRepository.getEarningsByDateBetween(
                TransactionType.WALLET_TOPUP,
                TransactionStatus.COMPLETED,
                from,
                to
        );
    }
}