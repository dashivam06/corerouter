package com.fleebug.corerouter.service.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleebug.corerouter.dto.billing.request.RecordUsageRequest;
import com.fleebug.corerouter.entity.apikey.ApiKey;
import com.fleebug.corerouter.entity.billing.BillingConfig;
import com.fleebug.corerouter.entity.billing.UsageRecord;
import com.fleebug.corerouter.entity.model.Model;
import com.fleebug.corerouter.entity.task.Task;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.billing.UsageUnitType;
import com.fleebug.corerouter.exception.apikey.ApiKeyLimitExceededException;
import com.fleebug.corerouter.repository.apikey.ApiKeyRepository;
import com.fleebug.corerouter.repository.billing.UsageRecordRepository;
import com.fleebug.corerouter.repository.model.ModelRepository;
import com.fleebug.corerouter.repository.task.TaskRepository;
import com.fleebug.corerouter.service.otp.OtpService;
import com.fleebug.corerouter.service.redis.RedisService;
import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageServiceLimitTest {

    @Mock private TelemetryClient telemetryClient;
    @Mock private UsageRecordRepository usageRecordRepository;
    @Mock private ApiKeyRepository apiKeyRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private ModelRepository modelRepository;
    @Mock private BillingConfigService billingConfigService;
    @Mock private TaskBillingService taskBillingService;
    @Mock private OtpService otpService;
    @Mock private RedisService redisService;
    
    @org.mockito.Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private UsageService usageService;

    private User testUser;
    private ApiKey testApiKey;
    private Task testTask;
    private Model testModel;
    private BillingConfig testConfig;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1)
                .email("test@example.com")
                .fullName("Test User")
                .build();

        testApiKey = ApiKey.builder()
                .apiKeyId(10)
                .user(testUser)
                .description("Test Key")
                .monthlyLimit(100)
                .dailyLimit(10)
                .build();

        testModel = Model.builder()
                .modelId(1)
                .build();

        testTask = Task.builder()
                .taskId("task-1")
                .apiKey(testApiKey)
                .model(testModel)
                .build();

        testConfig = BillingConfig.builder()
                .pricingMetadata("{\"rate\":0.01}")
                .build();
    }

    @Test
    // Tests that when monthly limit is exactly met/exceeded (100%), the threshold email job is queued
    void recordUsage_whenMonthlyLimitExceeded_sends100PercentEmailAlert() {
        RecordUsageRequest request = new RecordUsageRequest();
        request.setTaskId("task-1");
        request.setUsageUnitType(UsageUnitType.INPUT_TOKENS);
        request.setQuantity(new BigDecimal("1"));

        when(taskRepository.findByTaskId("task-1")).thenReturn(Optional.of(testTask));
        when(billingConfigService.getBillingConfigEntityByModelId(1)).thenReturn(testConfig);
        when(usageRecordRepository.save(any(UsageRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        when(usageRecordRepository.countByApiKeyApiKeyIdAndRecordedAtBetween(eq(10), any(), any()))
                .thenReturn(0L).thenReturn(99L).thenReturn(100L);

        when(redisService.existsInCache(anyString())).thenReturn(false);

        usageService.recordUsage(request);

        verify(otpService).publishApiKeyMonthlyUsageAlert(
                eq("test@example.com"),
                eq("Test User"),
                eq(1),
                eq(10),
                eq("Test Key"),
                eq(100),
                eq(100L),
                eq(100) // 100% notification
        );

        verify(redisService).saveToCache(contains(":100"), eq("true"), eq(40L), eq(TimeUnit.DAYS));
    }

    @Test
    // Tests that when limit crosses warning thresholds (80%, 90%), it fires appropriately without skipping ahead
    void recordUsage_whenMonthlyLimitAtWarningThreshold_sendsWarningEmailAlert() {
        RecordUsageRequest request = new RecordUsageRequest();
        request.setTaskId("task-1");
        request.setUsageUnitType(UsageUnitType.INPUT_TOKENS);
        request.setQuantity(new BigDecimal("10"));

        when(taskRepository.findByTaskId("task-1")).thenReturn(Optional.of(testTask));
        when(billingConfigService.getBillingConfigEntityByModelId(1)).thenReturn(testConfig);
        when(usageRecordRepository.save(any(UsageRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        when(usageRecordRepository.countByApiKeyApiKeyIdAndRecordedAtBetween(eq(10), any(), any()))
                .thenReturn(0L).thenReturn(85L).thenReturn(85L); // daily, monthly-block, monthly-email

        when(redisService.existsInCache(anyString())).thenReturn(false);

        usageService.recordUsage(request);

        verify(otpService).publishApiKeyMonthlyUsageAlert(
                eq("test@example.com"),
                eq("Test User"),
                eq(1),
                eq(10),
                eq("Test Key"),
                eq(100),
                eq(85L),
                eq(85) // 85% notification (as 85 >= 80 but < 90)
        );
        verify(redisService).saveToCache(contains(":80"), eq("true"), eq(40L), eq(TimeUnit.DAYS));
    }

    @Test
    // Tests that exceeding the daily limit throws an ApiKeyLimitExceededException
    void recordUsage_whenDailyLimitExceeded_throwsException() {
        RecordUsageRequest request = new RecordUsageRequest();
        request.setTaskId("task-1");
        request.setUsageUnitType(UsageUnitType.INPUT_TOKENS);
        request.setQuantity(new BigDecimal("2")); // previous usage 10 + 2 > daily limit 10

        when(taskRepository.findByTaskId("task-1")).thenReturn(Optional.of(testTask));

        // Let's pretend consumed today is already 10
        when(usageRecordRepository.countByApiKeyApiKeyIdAndRecordedAtBetween(eq(10), any(), any()))
                .thenReturn(10L); // 100% of the 10 daily limit

        ApiKeyLimitExceededException exception = assertThrows(ApiKeyLimitExceededException.class, () -> {
            usageService.recordUsage(request);
        });
        
        assertTrue(exception.getMessage().contains("Daily usage limit of 10 exceeded"), "Should mention daily limit exceeded");

        verify(usageRecordRepository, never()).save(any());
        verify(otpService, never()).publishApiKeyMonthlyUsageAlert(anyString(), anyString(), anyInt(), anyInt(), anyString(), anyInt(), anyLong(), anyInt());
    }

    @Test
    // Tests that exceeding the monthly limit throws an ApiKeyLimitExceededException setup directly
    void recordUsage_whenMonthlyLimitStrictlyExceeded_throwsExceptionBeforeSaving() {
        RecordUsageRequest request = new RecordUsageRequest();
        request.setTaskId("task-1");
        request.setUsageUnitType(UsageUnitType.INPUT_TOKENS);
        request.setQuantity(new BigDecimal("5")); 

        when(taskRepository.findByTaskId("task-1")).thenReturn(Optional.of(testTask));
        
        // Mock daily count = 0, but monthly = 100 
        when(usageRecordRepository.countByApiKeyApiKeyIdAndRecordedAtBetween(eq(10), any(), any()))
                .thenReturn(0L).thenReturn(100L);

        ApiKeyLimitExceededException exception = assertThrows(ApiKeyLimitExceededException.class, () -> {
            usageService.recordUsage(request);
        });

        assertTrue(exception.getMessage().contains("Monthly usage limit of 100 exceeded"), "Should mention monthly limit exceeded");

        verify(usageRecordRepository, never()).save(any());
    }
}
