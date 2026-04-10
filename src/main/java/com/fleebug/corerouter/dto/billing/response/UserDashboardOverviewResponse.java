package com.fleebug.corerouter.dto.billing.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "User Dashboard Overview Response", description = "Single payload for user dashboard cards and spending trend")
public class UserDashboardOverviewResponse {

    @Schema(description = "Current wallet balance", example = "0.00")
    private BigDecimal currentBalance;

    @Schema(description = "Active API key count", example = "3")
    private Long activeApiKeys;

    @Schema(description = "Tasks created in current month", example = "4")
    private Long tasksThisMonth;

    @Schema(description = "Today's consumption", example = "0.04")
    private BigDecimal todaysConsumption;

    @Schema(description = "Total spending for last 12 months", example = "120.25")
    private BigDecimal spendingLast12Months;

    @Schema(description = "Monthly spending trend for last 12 months in chronological order")
    private List<MonthlySpendingPoint> monthlySpendingTrend;
}