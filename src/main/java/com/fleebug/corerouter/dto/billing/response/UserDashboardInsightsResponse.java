package com.fleebug.corerouter.dto.billing.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "User Dashboard Insights Response", description = "Top card insights for user dashboard")
public class UserDashboardInsightsResponse {

    @Schema(description = "Current wallet balance", example = "0.00")
    private BigDecimal currentBalance;

    @Schema(description = "Active API key count", example = "3")
    private Long activeApiKeys;

    @Schema(description = "Tasks created in current month", example = "4")
    private Long tasksThisMonth;

    @Schema(description = "Today's consumption", example = "0.04")
    private BigDecimal todaysConsumption;
}