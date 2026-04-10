package com.fleebug.corerouter.service.otp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fleebug.corerouter.security.encryption.MessageEncryption;
import com.fleebug.corerouter.service.redis.RedisService;

import com.fleebug.corerouter.exception.apikey.RateLimitExceededException;
import com.fleebug.corerouter.exception.user.InvalidOtpException;
import com.fleebug.corerouter.exception.user.OtpExpiredException;
import com.fleebug.corerouter.enums.otp.AccountNotificationPurpose;
import com.fleebug.corerouter.enums.otp.ApiKeyUsageAlertPurpose;
import com.fleebug.corerouter.enums.otp.NotificationPurpose;
import com.fleebug.corerouter.enums.otp.OtpPurpose;

import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;



@Service
@RequiredArgsConstructor
public class OtpService {

    private final TelemetryClient telemetryClient;
    private final RedisService redisService;
    private final MessageEncryption messageEncryption;
    private final ObjectMapper objectMapper;

    @Value("${otp.length:6}")
    private int otpLength;

    @Value("${otp.ttl.minutes:5}")
    private long otpTtlMinutes;

    @Value("${otp.max.attempts:5}")
    private int maxAttempts;

    @Value("${verification.profile.completion.minutes:20}")
    private long verificationProfileCompletionMinutes;

    @Value("${otp.max.requests.per.email.per.hour:5}")
    private int maxRequestsPerEmailPerHour;

    // Redis Key Prefixes
    private static final String OTP_KEY_PREFIX = "otp:";
    private static final String VERIFICATION_EMAIL_PREFIX = "verification:email:";
    private static final String VERIFIED_PREFIX = "verified:";
    private static final String OTP_ATTEMPTS_PREFIX = "otp:attempts:";
    private static final String OTP_REQUEST_COUNT_PREFIX = "otp:request:count:";
    private static final String EMAIL_QUEUE_NAME = "queue:email";
    private static final String EMAIL_SCHEMA_VERSION = "v1";



    public String requestOtp(String email, OtpPurpose purpose, String fullName, Integer userId) {
        
        // log.debug("OTP_REQUEST | Action: CheckEmailRateLimit");

        // Check email rate limit
        String emailRateLimitKey = OTP_REQUEST_COUNT_PREFIX + email;
        String requestCountStr = redisService.getFromCache(emailRateLimitKey);
        long requestCount = requestCountStr != null ? Long.parseLong(requestCountStr) : 0;

        // log.debug("EMAIL_RATE_LIMIT_CHECK | CurrentCount: {} | MaxLimit: {}", 
        //          requestCount, maxRequestsPerEmailPerHour);

        if (requestCount >= maxRequestsPerEmailPerHour) {
            long ttlRemaining = redisService.getTTL(emailRateLimitKey);
            
            Map<String, String> properties = new HashMap<>();
            properties.put("email", email);
            properties.put("requestCount", String.valueOf(requestCount));
            properties.put("limit", String.valueOf(maxRequestsPerEmailPerHour));
            telemetryClient.trackTrace("OTP rate limit exceeded", SeverityLevel.Information, properties);
            
            throw new RateLimitExceededException("Too many OTP requests to this email. Maximum " + maxRequestsPerEmailPerHour + 
                    " requests allowed per hour. Try again in " + ttlRemaining + " seconds.");
        }

        // Initialize counter with TTL on first request
        if (requestCount == 0) {
            redisService.setCounterWithTTL(emailRateLimitKey, 1, TimeUnit.HOURS);
            // log.debug("EMAIL_RATE_LIMIT_COUNTER_CREATED | TTL: 1 hour");
        }

        // Increment counter for current request
        long newCount = redisService.incrementCounter(emailRateLimitKey);
        // log.debug("EMAIL_RATE_LIMIT_INCREMENTED | NewCount: {} | RemainingBudget: {}", 
        //          newCount, maxRequestsPerEmailPerHour - newCount);

        // Generate verificationId (UUID) and OTP
        String verificationId = UUID.randomUUID().toString();
        String otp = generateOtp();

        String encryptedOtp = messageEncryption.encrypt(otp);
        
        // log.debug("OTP_GENERATED | VerificationId: {}", verificationId);

        // Store OTP with TTL (5 minutes)
        String otpKey = OTP_KEY_PREFIX + verificationId;
        redisService.saveToCache(otpKey, encryptedOtp, otpTtlMinutes, TimeUnit.MINUTES);
        // log.debug("Stored OTP key: {}", otpKey);

        // Store email mapping with TTL (5 minutes)
        String emailKey = VERIFICATION_EMAIL_PREFIX + verificationId;
        redisService.saveToCache(emailKey, email, otpTtlMinutes, TimeUnit.MINUTES);
        // log.debug("Stored email key: {}", emailKey);

        // Initialize attempt counter
        String attemptsKey = OTP_ATTEMPTS_PREFIX + verificationId;
        redisService.setCounterWithTTL(attemptsKey, otpTtlMinutes, TimeUnit.MINUTES);
        // log.debug("Initialized attempts counter: {}", attemptsKey);

        // Publish to email queue
        publishOtpToQueue(email, otp, otpTtlMinutes, purpose, verificationId, fullName, userId);

        Map<String, String> properties = new HashMap<>();
        properties.put("verificationId", verificationId);
        properties.put("remainingRequests", String.valueOf(maxRequestsPerEmailPerHour - newCount));
        telemetryClient.trackTrace("OTP requested successfully", SeverityLevel.Information, properties);
        
        return verificationId;
    }

    
    public String validateOtp(String verificationId, String otp) {
        // Checking attempt counter
        String attemptsKey = OTP_ATTEMPTS_PREFIX + verificationId;
        String attemptsStr = redisService.getFromCache(attemptsKey);
        long attempts = attemptsStr != null ? Long.parseLong(attemptsStr) : 0;

        if (attempts >= maxAttempts) {
            Map<String, String> properties = new HashMap<>();
            properties.put("verificationId", verificationId);
            properties.put("attempts", String.valueOf(attempts));
            telemetryClient.trackTrace("Max OTP attempts exceeded", SeverityLevel.Information, properties);
            
            // Clean up keys
            redisService.deleteFromCache(OTP_KEY_PREFIX + verificationId);
            redisService.deleteFromCache(attemptsKey);
            redisService.deleteFromCache(VERIFICATION_EMAIL_PREFIX + verificationId);
            
            throw new InvalidOtpException("Max OTP attempts exceeded. Please request a new OTP.");
        }

        // Get OTP from cache
        String otpKey = OTP_KEY_PREFIX + verificationId;
        String cachedOtp = redisService.getFromCache(otpKey);

        if (cachedOtp == null || cachedOtp.isBlank()) {
            Map<String, String> properties = new HashMap<>();
            properties.put("verificationId", verificationId);
            telemetryClient.trackTrace("OTP not found or expired", SeverityLevel.Information, properties);
            throw new OtpExpiredException();
        }

        String decryptedOtp;
        try {
            decryptedOtp = messageEncryption.decrypt(cachedOtp);
        } catch (RuntimeException ex) {
            Map<String, String> properties = new HashMap<>();
            properties.put("verificationId", verificationId);
            telemetryClient.trackTrace("OTP decrypt failed for verificationId", SeverityLevel.Warning, properties);
            throw new InvalidOtpException("Invalid OTP session. Please request a new OTP.");
        }

        if (decryptedOtp == null) {
            Map<String, String> properties = new HashMap<>();
            properties.put("verificationId", verificationId);
            telemetryClient.trackTrace("OTP decrypt returned null", SeverityLevel.Information, properties);
            throw new OtpExpiredException();
        }

        // Validate OTP
        if (!MessageDigest.isEqual(decryptedOtp.getBytes(StandardCharsets.UTF_8), otp.getBytes(StandardCharsets.UTF_8))) {
            Map<String, String> properties = new HashMap<>();
            properties.put("verificationId", verificationId);
            properties.put("attempts", String.valueOf(attempts + 1));
            telemetryClient.trackTrace("Invalid OTP validation attempt", SeverityLevel.Information, properties);
            
            redisService.incrementCounter(attemptsKey);
            long remainingAttempts = maxAttempts - attempts - 1;
            throw new InvalidOtpException("Invalid OTP. Attempts remaining: " + remainingAttempts);
        }

        Map<String, String> properties = new HashMap<>();
        properties.put("verificationId", verificationId);
        telemetryClient.trackTrace("OTP validated successfully", SeverityLevel.Information, properties);

        // Get email
        String emailKey = VERIFICATION_EMAIL_PREFIX + verificationId;
        String email = redisService.getFromCache(emailKey);

        if (email == null) {
            // log.error("Email not found for verificationId: {}", verificationId);
            telemetryClient.trackTrace("Email not found for successful OTP verification", SeverityLevel.Information, properties);
            throw new OtpExpiredException("Verification session expired.");
        }

        // Set verified flag (20 minutes TTL - user has 20 min to complete registration)
        String verifiedKey = VERIFIED_PREFIX + verificationId;
        redisService.saveToCache(verifiedKey, "true", verificationProfileCompletionMinutes, TimeUnit.MINUTES);
        // log.debug("Set verified flag for verificationId: {} with TTL: {} minutes", verificationId, verificationProfileCompletionMinutes);

        // Delete OTP and attempts (no longer needed)
        redisService.deleteFromCache(otpKey);
        redisService.deleteFromCache(attemptsKey);
        // log.debug("Cleaned up OTP keys for verificationId: {}", verificationId);

        return email;
    }

    /**
     * Check if verificationId is verified (proof token exists)
     * 
     * @param verificationId UUID from step 1
     * @return boolean - true if verified
     */
    public boolean isVerified(String verificationId) {
        String verifiedKey = VERIFIED_PREFIX + verificationId;
        String verified = redisService.getFromCache(verifiedKey);
        return verified != null && verified.equals("true");
    }

    /**
     * Get email from verificationId
     * 
     * @param verificationId UUID from step 1
     * @return email address
     */
    public String getEmail(String verificationId) {
        String emailKey = VERIFICATION_EMAIL_PREFIX + verificationId;
        return redisService.getFromCache(emailKey);
    }

    /**
     * Clean up verification data after successful registration
     * 
     * @param verificationId UUID
     */
    public void cleanupVerification(String verificationId) {
        // log.info("Cleaning up verification data for verificationId: {}", verificationId);
        
        redisService.deleteFromCache(OTP_KEY_PREFIX + verificationId);
        redisService.deleteFromCache(VERIFICATION_EMAIL_PREFIX + verificationId);
        redisService.deleteFromCache(VERIFIED_PREFIX + verificationId);
        redisService.deleteFromCache(OTP_ATTEMPTS_PREFIX + verificationId);
        
        // log.debug("Cleaned up all verification keys for verificationId: {}", verificationId);
    }

    /**
     * Generate random OTP
     */
    private String generateOtp() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        
        return otp.toString();
    }

    /**
     * Publish OTP to email queue
     */
    private void publishOtpToQueue(String email,
                                   String otp,
                                   long otpTtlMinutes,
                                   OtpPurpose purpose,
                                   String verificationId,
                                   String fullName,
                                   Integer userId) {
        try {
            OtpPurpose resolvedPurpose = purpose == null ? OtpPurpose.REGISTRATION : purpose;
            String messageType = switch (resolvedPurpose) {
                case PASSWORD_RESET -> "PASSWORD_RESET_OTP";
                case REGISTRATION -> "REGISTRATION_OTP";
            };

            String resolvedName = (fullName == null || fullName.isBlank())
                    ? (email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : "User")
                    : fullName;
            Instant now = Instant.now();

            Map<String, Object> payload = new HashMap<>();
            payload.put("schemaVersion", EMAIL_SCHEMA_VERSION);
            payload.put("channel", "EMAIL");
            payload.put("category", "OTP");
            payload.put("templateKey", messageType);
            payload.put("type", messageType);
            payload.put("purpose", resolvedPurpose.name());
            payload.put("email", email);
            payload.put("fullName", resolvedName);
            payload.put("userId", userId);
            payload.put("verificationId", verificationId);
            payload.put("otp", otp);
            payload.put("otpTtlMinutes", otpTtlMinutes);
            payload.put("timestamp", now.toEpochMilli());
            payload.put("eventTime", now.toString());

            String message = messageEncryption.encrypt(objectMapper.writeValueAsString(payload));
            
            // Encrypt message before publishing
            redisService.publishToQueue(EMAIL_QUEUE_NAME, message);
            // log.info("OTP published to queue for email: {} (encrypted)", email);
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("email", email);
            telemetryClient.trackException(e, properties, null);
            throw new RuntimeException("Failed to queue email", e);
        }
    }

    public void publishPasswordChangedNotification(String email, String fullName, Integer userId) {
        try {
            String safeName = (fullName == null || fullName.isBlank()) ? "User" : fullName;
            Instant now = Instant.now();

            Map<String, Object> payload = new HashMap<>();
            payload.put("schemaVersion", EMAIL_SCHEMA_VERSION);
            payload.put("channel", "EMAIL");
            payload.put("category", "SECURITY");
            payload.put("templateKey", "PASSWORD_CHANGED_NOTIFICATION");
            payload.put("type", "PASSWORD_CHANGED_NOTIFICATION");
            payload.put("purpose", NotificationPurpose.PASSWORD_CHANGED.name());
            payload.put("email", email);
            payload.put("fullName", safeName);
            payload.put("userId", userId);
            payload.put("subject", "Password changed successfully");
            payload.put("message", "Hi " + safeName + ", your account password was changed successfully. If this was not you, please reset your password immediately.");
            payload.put("timestamp", now.toEpochMilli());
            payload.put("eventTime", now.toString());

            String encryptedMessage = messageEncryption.encrypt(objectMapper.writeValueAsString(payload));
            redisService.publishToQueue(EMAIL_QUEUE_NAME, encryptedMessage);
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("email", email);
            telemetryClient.trackException(e, properties, null);
            throw new RuntimeException("Failed to queue password changed notification", e);
        }
    }

    public void publishUserDeletedNotification(String email, String fullName, Integer userId, String deletedBy) {
        try {
            String safeName = (fullName == null || fullName.isBlank()) ? "User" : fullName;
            String safeDeletedBy = (deletedBy == null || deletedBy.isBlank()) ? "system" : deletedBy;
            Instant now = Instant.now();

            Map<String, Object> payload = new HashMap<>();
            payload.put("schemaVersion", EMAIL_SCHEMA_VERSION);
            payload.put("channel", "EMAIL");
            payload.put("category", "ACCOUNT");
            payload.put("templateKey", "USER_DELETED_NOTIFICATION");
            payload.put("type", "USER_DELETED_NOTIFICATION");
            payload.put("purpose", AccountNotificationPurpose.USER_DELETED.name());
            payload.put("email", email);
            payload.put("fullName", safeName);
            payload.put("userId", userId);
            payload.put("deletedBy", safeDeletedBy);
            payload.put("subject", "Account deleted");
            payload.put("message", "Hi " + safeName + ", your account has been deleted by " + safeDeletedBy + ". If you believe this was a mistake, please contact support.");
            payload.put("timestamp", now.toEpochMilli());
            payload.put("eventTime", now.toString());

            String encryptedMessage = messageEncryption.encrypt(objectMapper.writeValueAsString(payload));
            redisService.publishToQueue(EMAIL_QUEUE_NAME, encryptedMessage);
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("email", email);
            telemetryClient.trackException(e, properties, null);
            throw new RuntimeException("Failed to queue account deleted notification", e);
        }
    }

    public void publishApiKeyMonthlyUsageAlert(String email,
                                               String fullName,
                                               Integer userId,
                                               Integer apiKeyId,
                                               String apiKeyName,
                                               int monthlyLimit,
                                               long consumed,
                                               int thresholdPercent) {
        try {
            String safeName = (fullName == null || fullName.isBlank()) ? "User" : fullName;
            String safeApiKeyName = (apiKeyName == null || apiKeyName.isBlank())
                    ? "API Key #" + apiKeyId
                    : apiKeyName;
            ApiKeyUsageAlertPurpose purpose = switch (thresholdPercent) {
                case 80 -> ApiKeyUsageAlertPurpose.MONTHLY_LIMIT_80;
                case 90 -> ApiKeyUsageAlertPurpose.MONTHLY_LIMIT_90;
                default -> ApiKeyUsageAlertPurpose.MONTHLY_LIMIT_REACHED;
            };

            String subject = switch (purpose) {
                case MONTHLY_LIMIT_80 -> "API key usage at 80% of monthly limit";
                case MONTHLY_LIMIT_90 -> "API key usage at 90% of monthly limit";
                case MONTHLY_LIMIT_REACHED -> "API key monthly limit reached";
            };

            Instant now = Instant.now();

            Map<String, Object> payload = new HashMap<>();
            payload.put("schemaVersion", EMAIL_SCHEMA_VERSION);
            payload.put("channel", "EMAIL");
            payload.put("category", "BILLING");
            payload.put("templateKey", "API_KEY_MONTHLY_USAGE_ALERT");
            payload.put("type", "API_KEY_MONTHLY_USAGE_ALERT");
            payload.put("purpose", purpose.name());
            payload.put("email", email);
            payload.put("fullName", safeName);
            payload.put("userId", userId);
            payload.put("apiKeyId", apiKeyId);
            payload.put("apiKeyName", safeApiKeyName);
            payload.put("monthlyLimit", monthlyLimit);
            payload.put("consumed", consumed);
            payload.put("thresholdPercent", thresholdPercent);
            payload.put("subject", subject);
            payload.put("message", "Hi " + safeName + ", your API key " + safeApiKeyName + " (ID: " + apiKeyId + ") has used " + consumed + " of " + monthlyLimit + " monthly requests (" + thresholdPercent + "%). " + (thresholdPercent >= 100 ? "The monthly limit is now reached for this API key." : "Please review your usage before the month ends."));
            payload.put("timestamp", now.toEpochMilli());
            payload.put("eventTime", now.toString());

            String encryptedMessage = messageEncryption.encrypt(objectMapper.writeValueAsString(payload));
            redisService.publishToQueue(EMAIL_QUEUE_NAME, encryptedMessage);
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("email", email);
            properties.put("apiKeyId", apiKeyId == null ? "null" : String.valueOf(apiKeyId));
            telemetryClient.trackException(e, properties, null);
            throw new RuntimeException("Failed to queue api key monthly usage alert", e);
        }
    }
}
