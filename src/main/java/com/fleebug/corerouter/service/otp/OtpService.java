package com.fleebug.corerouter.service.otp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fleebug.corerouter.service.redis.RedisService;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;



@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final RedisService redisService;

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


   
    public String requestOtp(String email) {
        
        log.info("OTP_REQUEST | Email: {} | Action: CheckEmailRateLimit", email);

        // Check email rate limit
        String emailRateLimitKey = OTP_REQUEST_COUNT_PREFIX + email;
        String requestCountStr = redisService.getFromCache(emailRateLimitKey);
        long requestCount = requestCountStr != null ? Long.parseLong(requestCountStr) : 0;

        log.debug("EMAIL_RATE_LIMIT_CHECK | Email: {} | CurrentCount: {} | MaxLimit: {}", 
                email, requestCount, maxRequestsPerEmailPerHour);

        if (requestCount >= maxRequestsPerEmailPerHour) {
            long ttlRemaining = redisService.getTTL(emailRateLimitKey);
            log.warn("EMAIL_RATE_LIMIT_EXCEEDED | Email: {} | Requests: {} | MaxLimit: {} | RetryIn: {} seconds", 
                    email, requestCount, maxRequestsPerEmailPerHour, ttlRemaining);
            throw new IllegalArgumentException("Too many OTP requests to this email. Maximum " + maxRequestsPerEmailPerHour + 
                    " requests allowed per hour. Try again in " + ttlRemaining + " seconds.");
        }

        // Initialize counter with TTL on first request
        if (requestCount == 0) {
            redisService.setCounterWithTTL(emailRateLimitKey, 1, TimeUnit.HOURS);
            log.debug("EMAIL_RATE_LIMIT_COUNTER_CREATED | Email: {} | TTL: 1 hour", email);
        }

        // Increment counter for current request
        long newCount = redisService.incrementCounter(emailRateLimitKey);
        log.info("EMAIL_RATE_LIMIT_INCREMENTED | Email: {} | NewCount: {} | RemainingBudget: {}", 
                email, newCount, maxRequestsPerEmailPerHour - newCount);

        // Generate verificationId (UUID) and OTP
        String verificationId = UUID.randomUUID().toString();
        String otp = generateOtp();
        
        log.info("OTP_GENERATED | VerificationId: {} | Email: {}", verificationId, email);

        // Store OTP with TTL (5 minutes)
        String otpKey = OTP_KEY_PREFIX + verificationId;
        redisService.saveToCache(otpKey, otp, otpTtlMinutes, TimeUnit.MINUTES);
        log.debug("Stored OTP key: {}", otpKey);

        // Store email mapping with TTL (5 minutes)
        String emailKey = VERIFICATION_EMAIL_PREFIX + verificationId;
        redisService.saveToCache(emailKey, email, otpTtlMinutes, TimeUnit.MINUTES);
        log.debug("Stored email key: {}", emailKey);

        // Initialize attempt counter
        String attemptsKey = OTP_ATTEMPTS_PREFIX + verificationId;
        redisService.setCounterWithTTL(attemptsKey, otpTtlMinutes, TimeUnit.MINUTES);
        log.debug("Initialized attempts counter: {}", attemptsKey);

        // Publish to email queue
        publishOtpToQueue(email, otp, otpTtlMinutes);

        log.info("OTP_REQUEST_SUCCESS | Email: {} | VerificationId: {} | RemainingRequests: {}", 
                email, verificationId, maxRequestsPerEmailPerHour - newCount);
        return verificationId;
    }

    
    public String validateOtp(String verificationId, String otp) {
        log.info("Validating OTP for verificationId: {}", verificationId);

        // Check attempt counter
        String attemptsKey = OTP_ATTEMPTS_PREFIX + verificationId;
        String attemptsStr = redisService.getFromCache(attemptsKey);
        long attempts = attemptsStr != null ? Long.parseLong(attemptsStr) : 0;

        if (attempts >= maxAttempts) {
            log.warn("Max OTP attempts exceeded for verificationId: {}", verificationId);
            
            // Clean up keys
            redisService.deleteFromCache(OTP_KEY_PREFIX + verificationId);
            redisService.deleteFromCache(attemptsKey);
            redisService.deleteFromCache(VERIFICATION_EMAIL_PREFIX + verificationId);
            
            throw new IllegalArgumentException("Max OTP attempts exceeded. Please request a new OTP.");
        }

        // Get OTP from cache
        String otpKey = OTP_KEY_PREFIX + verificationId;
        String cachedOtp = redisService.getFromCache(otpKey);

        if (cachedOtp == null) {
            log.warn("OTP not found or expired for verificationId: {}", verificationId);
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }

        // Validate OTP
        if (!cachedOtp.equals(otp)) {
            log.warn("Invalid OTP attempt for verificationId: {}", verificationId);
            redisService.incrementCounter(attemptsKey);
            long remainingAttempts = maxAttempts - attempts - 1;
            throw new IllegalArgumentException("Invalid OTP. Attempts remaining: " + remainingAttempts);
        }

        log.info("OTP validated successfully for verificationId: {}", verificationId);

        // Get email
        String emailKey = VERIFICATION_EMAIL_PREFIX + verificationId;
        String email = redisService.getFromCache(emailKey);

        if (email == null) {
            log.error("Email not found for verificationId: {}", verificationId);
            throw new IllegalArgumentException("Verification session expired.");
        }

        // Set verified flag (20 minutes TTL - user has 20 min to complete registration)
        String verifiedKey = VERIFIED_PREFIX + verificationId;
        redisService.saveToCache(verifiedKey, "true", verificationProfileCompletionMinutes, TimeUnit.MINUTES);
        log.debug("Set verified flag for verificationId: {} with TTL: {} minutes", verificationId, verificationProfileCompletionMinutes);

        // Delete OTP and attempts (no longer needed)
        redisService.deleteFromCache(otpKey);
        redisService.deleteFromCache(attemptsKey);
        log.debug("Cleaned up OTP keys for verificationId: {}", verificationId);

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
        log.info("Cleaning up verification data for verificationId: {}", verificationId);
        
        redisService.deleteFromCache(OTP_KEY_PREFIX + verificationId);
        redisService.deleteFromCache(VERIFICATION_EMAIL_PREFIX + verificationId);
        redisService.deleteFromCache(VERIFIED_PREFIX + verificationId);
        redisService.deleteFromCache(OTP_ATTEMPTS_PREFIX + verificationId);
        
        log.debug("Cleaned up all verification keys for verificationId: {}", verificationId);
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
    private void publishOtpToQueue(String email, String otp, long otpTtlMinutes) {
        try {
            String message = String.format(
                    "{\"email\":\"%s\",\"otp\":\"%s\",\"type\":\"OTP_VERIFICATION\",\"timestamp\":%d,\"otpTtlMinutes\":%d}",
                    email, otp, System.currentTimeMillis(), otpTtlMinutes
            );
            
            redisService.publishToQueue(EMAIL_QUEUE_NAME, message);
            log.info("OTP published to queue for email: {}", email);
        } catch (Exception e) {
            log.error("Failed to publish OTP to queue for email: {}", email, e);
            throw new RuntimeException("Failed to queue email", e);
        }
    }
}
