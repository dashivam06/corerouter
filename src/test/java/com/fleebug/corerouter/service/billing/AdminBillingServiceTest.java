package com.fleebug.corerouter.service.billing;

import com.fleebug.corerouter.dto.billing.response.UsageSummaryResponse;
import com.fleebug.corerouter.enums.billing.UsageUnitType;
import com.fleebug.corerouter.entity.model.Model;
import com.fleebug.corerouter.enums.task.TaskStatus;
import com.fleebug.corerouter.repository.billing.UsageRecordRepository;
import com.fleebug.corerouter.repository.task.TaskRepository;
import com.fleebug.corerouter.repository.model.ModelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBillingServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UsageRecordRepository usageRecordRepository;

    @Mock
    private ModelRepository modelRepository;

    @InjectMocks
    private UsageService usageService;

    @Test
    // Tests that the total cost for all users is retrieved successfully across the platform
    void getTotalCostForAllUsers_ReturnsCostSuccessfully() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        BigDecimal expectedCost = new BigDecimal("450.50");

        when(taskRepository.sumChargedCostByStatusAndCompletedAtBetween(
                eq(TaskStatus.COMPLETED), eq(from), eq(to)))
                .thenReturn(expectedCost);

        BigDecimal actualCost = usageService.getTotalCostForAllUsers(from, to);

        assertEquals(expectedCost, actualCost);
    }

    @Test
    // Tests that the usage summary for a specific API key is correctly grouped by unit type
    void getUsageSummaryByApiKey_ReturnsSummaryProperlyGrouped() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        
        Object[] row1 = new Object[] { UsageUnitType.INPUT_TOKENS, new BigDecimal("100"), new BigDecimal("10.00") };
        Object[] row2 = new Object[] { UsageUnitType.OUTPUT_TOKENS, new BigDecimal("50"), new BigDecimal("5.00") };

        when(usageRecordRepository.sumUsageByApiKeyGroupedByUnitType(10, from, to))
                .thenReturn(List.of(row1, row2));

        UsageSummaryResponse response = usageService.getUsageSummaryByApiKey(10, from, to);

        assertNotNull(response);
        assertEquals(new BigDecimal("15.00"), response.getTotalCost());
        assertEquals(2, response.getBreakdown().size());
        assertEquals(new BigDecimal("100"), response.getBreakdown().get(0).getTotalQuantity());
        assertEquals(new BigDecimal("5.00"), response.getBreakdown().get(1).getTotalCost());
    }

    @Test
    // Tests that the usage summary for a specific API key is accurately grouped and aggregated by AI model
    void getUsageSummaryByApiKeyGroupedByModel_ReturnsModelSummaries() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        
        Object[] row1 = new Object[] { 1, UsageUnitType.INPUT_TOKENS, new BigDecimal("100"),
             new BigDecimal("10.00"), 5L };
        Object[] row2 = new Object[] { 2, UsageUnitType.OUTPUT_TOKENS, new BigDecimal("50"),
             new BigDecimal("5.00"), 3L };

        Model modelA = Model.builder().modelId(1).fullname("GPT-4").build();
        Model modelB = Model.builder().modelId(2).fullname("Claude-3").build();

        when(usageRecordRepository.sumUsageByApiKeyGroupedByModelAndUnitType(10, from, to))
                .thenReturn(List.of(row1, row2));

        when(modelRepository.findById(1)).thenReturn(Optional.of(modelA));
        when(modelRepository.findById(2)).thenReturn(Optional.of(modelB));

        UsageSummaryResponse response = usageService.getUsageSummaryByApiKeyGroupedByModel(10, from, to);

        assertNotNull(response);
        assertEquals(new BigDecimal("15.00"), response.getTotalCost());
        assertEquals(2, response.getBreakdown().size());
        assertEquals("GPT-4", response.getBreakdown().get(0).getModelName());
        assertEquals("Claude-3", response.getBreakdown().get(1).getModelName());
    }
}
