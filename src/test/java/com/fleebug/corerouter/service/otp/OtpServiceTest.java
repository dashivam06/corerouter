package com.fleebug.corerouter.service.otp;

import com.fleebug.corerouter.enums.otp.OtpPurpose;
import com.fleebug.corerouter.exception.apikey.RateLimitExceededException;
import com.fleebug.corerouter.exception.user.InvalidOtpException;
import com.fleebug.corerouter.exception.user.OtpExpiredException;
import com.fleebug.corerouter.service.redis.RedisService;
import com.fleebug.corerouter.security.encryption.MessageEncryption;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OtpServiceTest {

    private TelemetryClient telemetryClient;
    private RedisService redisService;
    private MessageEncryption messageEncryption;
    private ObjectMapper objectMapper;
    private OtpService otpService;

    @BeforeEach
    void setUp() throws Exception {
        telemetryClient = mock(TelemetryClient.class);
        redisService = mock(RedisService.class);
        messageEncryption = mock(MessageEncryption.class);
        objectMapper = new ObjectMapper();

        otpService = new OtpService(telemetryClient, redisService, messageEncryption, objectMapper);

        // Configure @Value fields via reflection for predictable behaviour
        setField("otpLength", 6);
        setField("otpTtlMinutes", 5L);
        setField("maxAttempts", 3);
        setField("verificationProfileCompletionMinutes", 20L);
        setField("maxRequestsPerEmailPerHour", 5);
    }

    private void setField(String name, Object value) throws Exception {
        Field field = OtpService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(otpService, value);
    }

    // Expired or missing OTP should result in OtpExpiredException
    @Test
    void validateOtp_whenOtpMissing_throwsOtpExpiredException() {
        String verificationId = "ver-123";
        String attemptsKey = "otp:attempts:" + verificationId;
        String otpKey = "otp:" + verificationId;

        // First getFromCache call for attempts -> "0", second for OTP value -> null
        when(redisService.getFromCache(attemptsKey)).thenReturn("0");
        when(redisService.getFromCache(otpKey)).thenReturn(null);

        assertThrows(OtpExpiredException.class, () ->
                otpService.validateOtp(verificationId, "123456")
        );
    }

    // Wrong OTP code should bump attempts and throw InvalidOtpException
    @Test
    void validateOtp_whenCodeIsWrong_incrementsAttemptsAndThrowsInvalidOtpException() {
        String verificationId = "ver-456";
        String attemptsKey = "otp:attempts:" + verificationId;
        String otpKey = "otp:" + verificationId;

        // No attempts yet
        when(redisService.getFromCache(attemptsKey)).thenReturn("0");

        // Cached OTP decrypts to 123456 but user sends 000000
        when(redisService.getFromCache(otpKey)).thenReturn("encrypted-otp");
        when(messageEncryption.decrypt("encrypted-otp")).thenReturn("123456");

        assertThrows(InvalidOtpException.class, () ->
                otpService.validateOtp(verificationId, "000000")
        );

        verify(redisService).incrementCounter(attemptsKey);
    }

    // Successful OTP request should cache data and queue email job
    @Test
    void requestOtp_whenUnderRateLimit_cachesOtpAndQueuesEmailJob() {
        String email = "uesc.barsha@gmail.com";
        String fullName = "Barsha Supriya";
        Integer userId = 44444;

        String rateLimitKey = "otp:request:count:" + email;

        // No prior requests for this email
        when(redisService.getFromCache(rateLimitKey)).thenReturn(null);
        when(redisService.incrementCounter(rateLimitKey)).thenReturn(1L);

        // Encrypt OTP and email payload
        when(messageEncryption.encrypt(anyString())).thenReturn("encrypted-value");

        String verificationId = otpService.requestOtp(email, OtpPurpose.REGISTRATION, fullName, userId);

        assertNotNull(verificationId);
        assertFalse(verificationId.isBlank());

        // OTP value and email mapping cached with TTL
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisService, atLeastOnce()).saveToCache(keyCaptor.capture(), anyString(), eq(5L), eq(TimeUnit.MINUTES));

        List<String> keys = keyCaptor.getAllValues();
        assertTrue(keys.stream().anyMatch(k -> k.startsWith("otp:")), 
                "OTP key should be stored");
        assertTrue(keys.stream().anyMatch(k -> k.startsWith("verification:email:")), 
                "Verification email key should be stored");

        // Attempt counter initialised
        verify(redisService).setCounterWithTTL(startsWith("otp:attempts:"), eq(5L), eq(TimeUnit.MINUTES));

        // Email job published to queue
        verify(redisService).publishToQueue(eq("queue:email"), anyString());
    }

    // Hitting the per-email rate limit should block OTP requests
    @Test
    void requestOtp_whenRateLimitExceeded_throwsExceptionAndDoesNotQueueEmailJob() {
        String email = "limited@example.com";
        String rateLimitKey = "otp:request:count:" + email;

        // Simulate user already at or above limit
        when(redisService.getFromCache(rateLimitKey)).thenReturn("5");
        when(redisService.getTTL(rateLimitKey)).thenReturn(60L);

        assertThrows(RateLimitExceededException.class, () ->
                otpService.requestOtp(email, OtpPurpose.REGISTRATION, null, null)
        );

        // No queue push when rate limited
        verify(redisService, never()).publishToQueue(anyString(), anyString());
    }

    // Password change event should create a notification email job
    @Test
    void publishPasswordChangedNotification_queuesEmailJob() {
        when(messageEncryption.encrypt(anyString())).thenReturn("enc");

        otpService.publishPasswordChangedNotification("user@example.com", "John Doe", 1);

        verify(redisService).publishToQueue(eq("queue:email"), anyString());
    }

    // User deletion should create an account-deleted notification email job
    @Test
    void publishUserDeletedNotification_queuesEmailJob() {
        when(messageEncryption.encrypt(anyString())).thenReturn("enc");

        otpService.publishUserDeletedNotification("user@example.com", "Jane Doe", 2, "admin");

        verify(redisService).publishToQueue(eq("queue:email"), anyString());
    }

    // API key usage threshold should trigger a monthly usage alert email job
    @Test
    void publishApiKeyMonthlyUsageAlert_queuesEmailJob() {
        when(messageEncryption.encrypt(anyString())).thenReturn("enc");

        otpService.publishApiKeyMonthlyUsageAlert(
                "user@example.com",
                "API User",
                3,
                10,
                "Primary Key",
                1000,
                800,
                80
        );

        verify(redisService).publishToQueue(eq("queue:email"), anyString());
    }
}
