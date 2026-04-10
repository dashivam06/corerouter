package com.fleebug.corerouter.dto.billing.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "User Billing Insights Response", description = "Billing overview insights for authenticated user's dashboard")
public class UserBillingInsightsResponse {

    @Schema(description = "Current wallet balance", example = "2450.00")
    private BigDecimal currentBalance;

    @Schema(description = "Credits used in the current month", example = "18420.55")
    private BigDecimal creditsUsedThisMonth;

    @Schema(description = "Credits used change percent compared to equivalent period in last month", example = "21.0")
    private BigDecimal creditsUsedChangeFromLastMonthPercent;

    @Schema(description = "Active API key count", example = "3")
    private Long activeApiKeys;

    @Schema(description = "Tasks created in the current month", example = "27")
    private Long tasksThisMonth;

    @Schema(description = "Total consumption recorded today", example = "12.45")
    private BigDecimal todaysConsumption;

    @Schema(description = "Requested spending filter period", example = "month")
    private String spendingPeriod;

    @Schema(description = "Start time for selected spending period")
    private LocalDateTime spendingPeriodStart;

    @Schema(description = "End time for selected spending period")
    private LocalDateTime spendingPeriodEnd;

    @Schema(description = "Total spending for selected period", example = "210.25")
    private BigDecimal spendingInSelectedPeriod;

    @Schema(description = "Current period label", example = "30days")
    private String currentPeriod;

    @Schema(description = "Total spend for current period", example = "0.15")
    private BigDecimal totalSpend;

    @Schema(description = "Total request count for current period", example = "42")
    private Long totalRequestsCurrentPeriod;

    @Schema(description = "Average cost per request for current period", example = "0.00")
    private BigDecimal avgCostPerRequest;

    @Schema(description = "Usage request counts by model type for selected period")
    private Map<String, Long> usageByModelTypeCounts;
}