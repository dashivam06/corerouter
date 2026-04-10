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
import com.fleebug.corerouter.service.redis.RedisService;
import com.fleebug.corerouter.util.HttpClientUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TelemetryClient telemetryClient;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;
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

    private static final String TOPUP_SESSION_PREFIX = "payment:topup:session:";
    private static final long TOPUP_SESSION_TTL_MINUTES = 5;
    private static final String TOPUP_INVOICE_EMAIL_SKIP_PREFIX = "billing:topup:invoice:skip:";

    @Transactional
    public Map<String, String> initiateTopUp(User user, BigDecimal amount) {
        String transactionUuid = buildTransactionUuid(user.getUserId());

        Map<String, Object> sessionPayload = new HashMap<>();
        sessionPayload.put("userId", user.getUserId());
        sessionPayload.put("amount", amount.toPlainString());
        sessionPayload.put("transactionUuid", transactionUuid);
        sessionPayload.put("createdAt", LocalDateTime.now().toString());

        try {
            redisService.saveToCache(
                    TOPUP_SESSION_PREFIX + transactionUuid,
                    objectMapper.writeValueAsString(sessionPayload),
                    TOPUP_SESSION_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (JsonProcessingException e) {
            telemetryClient.trackException(e, Map.of("transactionUuid", transactionUuid), null);
            throw new TransactionVerificationException("Failed to initialize payment session", e);
        }

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

            String normalizedTransactionUuid = normalizeTransactionUuid(transactionUuid);

            Transaction existing = transactionRepository.findByEsewaTransactionId(normalizedTransactionUuid).orElse(null);
            if (existing != null && existing.getStatus() == TransactionStatus.COMPLETED) {
                telemetryClient.trackTrace("Transaction already completed: " + normalizedTransactionUuid,
                        SeverityLevel.Information,
                        Map.of("transactionUuid", normalizedTransactionUuid));
                return existing;
            }

            String sessionKey = TOPUP_SESSION_PREFIX + normalizedTransactionUuid;
            String sessionRaw = redisService.getFromCache(sessionKey);
            if (sessionRaw == null || sessionRaw.isBlank()) {
                throw new TransactionVerificationException("Payment session expired or not found");
            }

            JsonNode sessionNode = objectMapper.readTree(sessionRaw);
            Integer userId = sessionNode.path("userId").isInt() ? sessionNode.path("userId").asInt() : null;
            String stagedAmount = sessionNode.path("amount").asText(null);

            if (userId == null || stagedAmount == null || stagedAmount.isBlank()) {
                throw new TransactionVerificationException("Invalid payment session data");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new TransactionNotFoundException("User not found for payment session: " + userId));

            // Verify with eSewa API
            String verificationUrl = String.format("%s?product_code=%s&total_amount=%s&transaction_uuid=%s",
                    verifyUrl, merchantId, totalAmount, transactionUuid);

            telemetryClient.trackTrace("Verifying transaction with URL: " + verificationUrl, SeverityLevel.Information, Map.of("verificationUrl", verificationUrl));
            
            try {
                // eSewa status check returns JSON
                Map<String, Object> response = httpClientUtil.getJsonMap(verificationUrl, Map.of(), 5000, 10000);
                String verifiedStatus = (String) response.get("status");
                
                if ("COMPLETE".equals(verifiedStatus)) {
                    Transaction transaction = existing != null
                            ? existing
                            : Transaction.builder()
                                    .user(user)
                                    .amount(new BigDecimal(stagedAmount))
                                    .esewaTransactionId(normalizedTransactionUuid)
                                    .type(TransactionType.WALLET_TOPUP)
                                    .status(TransactionStatus.COMPLETED)
                                    .productCode(merchantId)
                                    .createdAt(LocalDateTime.now())
                                    .build();

                    transaction.setStatus(TransactionStatus.COMPLETED);
                    transaction.setCompletedAt(LocalDateTime.now());
                    transactionRepository.save(transaction);
                    redisService.saveToCache(TOPUP_INVOICE_EMAIL_SKIP_PREFIX + normalizedTransactionUuid, "true", 1, TimeUnit.DAYS);
                    redisService.deleteFromCache(sessionKey);

                    // Credit Wallet
                    user.setBalance(user.getBalance().add(transaction.getAmount()));
                    userRepository.save(user);
                    
                    telemetryClient.trackTrace("Wallet credited for user: " + user.getUserId(), SeverityLevel.Information, Map.of("userId", String.valueOf(user.getUserId()), "transactionUuid", normalizedTransactionUuid));
                    return transaction;
                } else {
                    telemetryClient.trackTrace("eSewa verification failed. Status: " + verifiedStatus, SeverityLevel.Warning, Map.of("verifiedStatus", verifiedStatus != null ? verifiedStatus : "null"));
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
            String normalized = normalizeTransactionUuid(transactionUuid);
            redisService.deleteFromCache(TOPUP_SESSION_PREFIX + normalized);
            telemetryClient.trackTrace("Payment cancelled for transaction: " + normalized,
                    SeverityLevel.Information,
                    Map.of("transactionUuid", normalized));
        } else {
            telemetryClient.trackTrace("Could not extract transaction UUID from failure callback", SeverityLevel.Information, null);
        }
    }

    private String buildTransactionUuid(Integer userId) {
        return userId + "_" + UUID.randomUUID();
    }

    private String normalizeTransactionUuid(String transactionUuid) {
        if (transactionUuid == null) {
            return null;
        }

        String trimmed = transactionUuid.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        return trimmed;
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
        // No-op: top-up sessions now expire in Redis with TTL and only COMPLETED transactions are persisted.
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

    @Transactional(readOnly = true)
    public BigDecimal getTopUpAmountAllTime() {
        return transactionRepository.sumAmountByTypeAndStatus(
                TransactionType.WALLET_TOPUP,
                TransactionStatus.COMPLETED
        );
    }

    @Transactional(readOnly = true)
    public Map<Integer, BigDecimal> getTopUpAmountByHour(LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = transactionRepository.sumAmountByTypeAndStatusAndCompletedAtBetweenGroupedByHour(
                TransactionType.WALLET_TOPUP,
                TransactionStatus.COMPLETED,
                from,
                to
        );

        Map<Integer, BigDecimal> result = new HashMap<>();
        for (Object[] row : rows) {
            Integer hour = ((Number) row[0]).intValue();
            BigDecimal amount = (BigDecimal) row[1];
            result.put(hour, amount);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionsByFilters(
            TransactionType type,
            TransactionStatus status,
            String search,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "completedAt"));

        Specification<Transaction> specification = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            predicates.add(cb.between(root.get("completedAt"), from, to));

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (search != null && !search.trim().isEmpty()) {
                String keyword = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                jakarta.persistence.criteria.Join<Object, Object> userJoin = root.join("user");
                predicates.add(cb.or(
                        cb.like(cb.lower(userJoin.get("fullName")), keyword),
                        cb.like(cb.lower(userJoin.get("email")), keyword),
                        cb.like(cb.lower(root.get("esewaTransactionId")), keyword)
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return transactionRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByFilters(
            TransactionType type,
            TransactionStatus status,
            LocalDateTime from,
            LocalDateTime to) {
        return getTransactionsByFilters(type, status, null, from, to, 0, 1000).getContent();
    }

    /**
     * Get daily earnings (sum of completed WALLET_TOPUP transactions grouped by date)
     */
    @Transactional(readOnly = true)
    public List<Object[]> getDailyEarnings(LocalDateTime from, LocalDateTime to) {
        List<Transaction> transactions = transactionRepository.findAllByTypeAndStatusAndCompletedAtBetween(
                TransactionType.WALLET_TOPUP,
                TransactionStatus.COMPLETED,
                from,
                to
        );

        Map<LocalDate, BigDecimal> amountByDate = new TreeMap<>(Comparator.reverseOrder());
        Map<LocalDate, Long> countByDate = new TreeMap<>(Comparator.reverseOrder());

        for (Transaction transaction : transactions) {
            if (transaction.getCompletedAt() == null) {
                continue;
            }

            LocalDate date = transaction.getCompletedAt().toLocalDate();
            BigDecimal amount = transaction.getAmount() == null ? BigDecimal.ZERO : transaction.getAmount();

            amountByDate.put(date, amountByDate.getOrDefault(date, BigDecimal.ZERO).add(amount));
            countByDate.put(date, countByDate.getOrDefault(date, 0L) + 1L);
        }

        List<Object[]> result = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> entry : amountByDate.entrySet()) {
            LocalDate date = entry.getKey();
            result.add(new Object[]{date, entry.getValue(), countByDate.getOrDefault(date, 0L)});
        }

        return result;
    }
}