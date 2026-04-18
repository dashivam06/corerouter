package com.fleebug.corerouter.service.billing;

import com.fleebug.corerouter.dto.billing.request.RecordUsageRequest;
import com.fleebug.corerouter.dto.billing.response.UsageRecordResponse;
import com.fleebug.corerouter.entity.apikey.ApiKey;
import com.fleebug.corerouter.entity.billing.BillingConfig;
import com.fleebug.corerouter.entity.billing.UsageRecord;
import com.fleebug.corerouter.entity.model.Model;
import com.fleebug.corerouter.entity.task.Task;
import com.fleebug.corerouter.enums.billing.UsageUnitType;
import com.fleebug.corerouter.repository.billing.UsageRecordRepository;
import com.fleebug.corerouter.repository.task.TaskRepository;
import com.microsoft.applicationinsights.TelemetryClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Spy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageServiceRecordTest {

    @Mock
    private UsageRecordRepository usageRecordRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private BillingConfigService billingConfigService;

    @Mock
    private TaskBillingService taskBillingService;

    @Mock
    private TelemetryClient telemetryClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private UsageService usageService;

    private Task task;
    private ApiKey apiKey;
    private Model model;
    private BillingConfig config;
    private RecordUsageRequest request;

    @BeforeEach
    void setUp() {
        apiKey = ApiKey.builder()
                .apiKeyId(1)
                .build();

        model = Model.builder()
                .modelId(10)
                .build();

        task = Task.builder()
                .taskId("test-task-123")
                .apiKey(apiKey)
                .model(model)
                .build();

        config = BillingConfig.builder()
                .pricingMetadata("{\"inputRate\": 0.005, \"outputRate\": 0.01}")
                .chargeMultiplier(BigDecimal.ONE)
                .build();

        request = new RecordUsageRequest("test-task-123", UsageUnitType.INPUT_TOKENS, new BigDecimal("1000"));
    }

    @Test
    // BILL-05,06: Usage record and ledger snapshot -> DB SELECT from usage_records table showing the new row.
    void recordUsage_CreatesNewRowAndCalculatesCostSnapshot() {
        // Arrange
        when(taskRepository.findByTaskId("test-task-123")).thenReturn(Optional.of(task));
        // Mock the billing config resolving logic
        when(billingConfigService.getBillingConfigEntityByModelId(10)).thenReturn(config);
        
        // Mock save logic to return the passed instance with an ID
        when(usageRecordRepository.save(any(UsageRecord.class))).thenAnswer(invocation -> {
            UsageRecord savedRecord = invocation.getArgument(0);
            savedRecord.setUsageId(999L);
            return savedRecord;
        });

        // Act
        UsageRecordResponse response = usageService.recordUsage(request);

        // Assert
        assertNotNull(response);
        // 1000 tokens * 0.005 token rate = 5.00
        BigDecimal expectedCost = new BigDecimal("1000").multiply(new BigDecimal("0.005")).setScale(10, RoundingMode.HALF_UP);
        
        assertEquals("test-task-123", response.getTaskId(), "Record belongs to the correct task");
        assertEquals(expectedCost, response.getCost(), "Usage record ledger snapshot perfectly captured quantity * rateMultiplier");
        assertEquals(new BigDecimal("1000"), response.getQuantity(), "Recorded quantity should map accurately");
        
        verify(usageRecordRepository).save(any(UsageRecord.class));
    }
}
