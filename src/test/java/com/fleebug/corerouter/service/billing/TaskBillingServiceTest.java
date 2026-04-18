package com.fleebug.corerouter.service.billing;

import com.fleebug.corerouter.entity.apikey.ApiKey;
import com.fleebug.corerouter.entity.billing.BillingConfig;
import com.fleebug.corerouter.entity.billing.UsageRecord;
import com.fleebug.corerouter.entity.model.Model;
import com.fleebug.corerouter.entity.task.Task;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.task.TaskStatus;
import com.fleebug.corerouter.exception.billing.BillingCalculationException;
import com.fleebug.corerouter.repository.billing.BillingConfigRepository;
import com.fleebug.corerouter.repository.billing.UsageRecordRepository;
import com.fleebug.corerouter.repository.task.TaskRepository;
import com.fleebug.corerouter.repository.user.UserRepository;
import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskBillingServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UsageRecordRepository usageRecordRepository;

    @Mock
    private BillingConfigRepository billingConfigRepository;

    @Mock
    private TelemetryClient telemetryClient;

    @InjectMocks
    private TaskBillingService taskBillingService;

    private User user;
    private ApiKey apiKey;
    private Model model;
    private Task task;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(1)
                .balance(new BigDecimal("10.00"))
                .build();

        apiKey = ApiKey.builder()
                .apiKeyId(1)
                .user(user)
                .build();

        model = Model.builder()
                .modelId(10)
                .build();

        task = Task.builder()
                .taskId("test-task-id")
                .apiKey(apiKey)
                .model(model)
                .status(TaskStatus.COMPLETED)
                .chargedCost(BigDecimal.ZERO)
                .build();
    }

    @Test
    // BILL-03,04: Cost calculation and wallet debit -> SELECT wallet balance BEFORE task -> task completion -> AFTER
    void applyDebitIfEligible_CalculatesCostAndDebitsWallet() {
        // Arrange
        BillingConfig config = BillingConfig.builder()
                .chargeMultiplier(new BigDecimal("1.5"))
                .build();

        UsageRecord record = UsageRecord.builder()
                .usageId(100L)
                .task(task)
                .cost(new BigDecimal("2.00")) // Raw cost
                .billingConfig(config)
                .build();

        when(userRepository.findByIdForUpdate(1)).thenReturn(Optional.of(user));
        when(usageRecordRepository.findByTask(task)).thenReturn(List.of(record));

        // Act
        taskBillingService.applyDebitIfEligible(task);

        // Assert
        // The raw cost is 2.00. Multiplier is 1.5. Target charged cost = 2.00 * 1.5 = 3.00.
        // Wallet before: 10.00 -> Wallet after: 10.00 - 3.00 = 7.00
        BigDecimal expectedBalance = new BigDecimal("7.00");
        assertEquals(expectedBalance, user.getBalance(), "User balance should be exactly deducted by target cost");
        assertEquals(new BigDecimal("3.00"), task.getChargedCost(), "Task should record the final charged deduction");
        assertEquals(expectedBalance, task.getRemainingBalance(), "Task ledger should snapshot the closing balance");
        
        verify(userRepository).save(user);
        verify(taskRepository).save(task);
    }

    @Test
    // BILL-05,06: Idempotent debit -> second charge attempt returns an error/duplicate rejection (no-op)
    void applyDebitIfEligible_IsIdempotent_DoesNotChargeAgain() {
        // Arrange
        task.setChargedCost(new BigDecimal("3.00")); // Task already charged previously
        
        BillingConfig config = BillingConfig.builder()
                .chargeMultiplier(new BigDecimal("1.5"))
                .build();

        UsageRecord record = UsageRecord.builder()
                .cost(new BigDecimal("2.00"))
                .billingConfig(config)
                .build();

        when(usageRecordRepository.findByTask(task)).thenReturn(List.of(record));

        // Act
        taskBillingService.applyDebitIfEligible(task);

        // Assert
        // Delta between target cost (3.00) and already charged (3.00) is 0. 
        // Debit logic returns early ensuring 100% idempotency.
        verify(userRepository, never()).findByIdForUpdate(any());
        verify(userRepository, never()).save(any());
        verify(taskRepository, never()).save(any());
    }

    @Test
    // BILL-05: Insufficient balance -> Postman showing 402 response for insufficient balance.
    void applyDebitIfEligible_ThrowsBillingCalculationException_WhenInsufficientBalance() {
        // Arrange
        user.setBalance(new BigDecimal("1.00")); // Only has 1.00 left

        BillingConfig config = BillingConfig.builder()
                .chargeMultiplier(BigDecimal.ONE)
                .build();

        UsageRecord record = UsageRecord.builder()
                .cost(new BigDecimal("2.50")) // Needs 2.50
                .billingConfig(config)
                .build();

        when(userRepository.findByIdForUpdate(1)).thenReturn(Optional.of(user));
        when(usageRecordRepository.findByTask(task)).thenReturn(List.of(record));

        // Act & Assert
        BillingCalculationException ex = assertThrows(BillingCalculationException.class, () -> {
            taskBillingService.applyDebitIfEligible(task);
        });

        assertTrue(ex.getMessage().contains("Insufficient balance for task completion"));
        assertTrue(ex.getMessage().contains("Required=2.50"));
        assertTrue(ex.getMessage().contains("available=1.00"));
        
        verify(userRepository, never()).save(user);
    }
}
