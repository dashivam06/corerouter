package com.fleebug.corerouter.service.billing;

import com.fleebug.corerouter.dto.billing.response.UserDashboardInsightsResponse;
import com.fleebug.corerouter.enums.apikey.ApiKeyStatus;
import com.fleebug.corerouter.enums.task.TaskStatus;
import com.fleebug.corerouter.repository.apikey.ApiKeyRepository;
import com.fleebug.corerouter.repository.billing.UsageRecordRepository;
import com.fleebug.corerouter.repository.task.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBillingServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private UsageRecordRepository usageRecordRepository;

    @InjectMocks
    private UsageService usageService;

    @Test
    // Tests that the total cumulative cost for a specific user is calculated correctly over a date range
    void getTotalCostByUser_ReturnsUserCost() {
        LocalDateTime from = LocalDateTime.now().minusDays(5);
        LocalDateTime to = LocalDateTime.now();
        BigDecimal expectedCost = new BigDecimal("45.50");

        when(taskRepository.sumChargedCostByUserAndStatusAndCompletedAtBetween(
                eq(1), eq(TaskStatus.COMPLETED), eq(from), eq(to)))
                .thenReturn(expectedCost);

        BigDecimal actualCost = usageService.getTotalCostByUser(1, from, to);

        assertEquals(expectedCost, actualCost);
    }

    @Test
    // Tests that the user dashboard insights correctly aggregate balance, active keys, and recent consumption
    void getUserDashboardInsights_ReturnsValidInsights() {
        Integer userId = 1;
        BigDecimal currentBalance = new BigDecimal("100.00");
        
        when(apiKeyRepository.countByUserUserIdAndStatus(eq(userId), eq(ApiKeyStatus.ACTIVE)))
                .thenReturn(3L);
        when(taskRepository.countByApiKey_User_UserIdAndCreatedAtBetween(eq(userId), 
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(150L);
        when(taskRepository.sumChargedCostByUserAndStatusAndCompletedAtBetween(eq(userId), 
                eq(TaskStatus.COMPLETED), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("12.50"));

        UserDashboardInsightsResponse response = usageService.getUserDashboardInsights(userId, currentBalance);

        assertNotNull(response);
        assertEquals(new BigDecimal("100.00"), response.getCurrentBalance());
        assertEquals(3L, response.getActiveApiKeys());
        assertEquals(150L, response.getTasksThisMonth());
        assertEquals(new BigDecimal("12.50"), response.getTodaysConsumption());
    }
}
