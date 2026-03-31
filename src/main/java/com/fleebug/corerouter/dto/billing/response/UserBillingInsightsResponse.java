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
@Schema(title = "User Billing Insights Response", description = "Billing overview insights for authenticated user's dashboard")
public class UserBillingInsightsResponse {

    @Schema(description = "Current wallet balance", example = "2450.00")
    private BigDecimal currentBalance;

    @Schema(description = "Credits used in the current month", example = "18420.55")
    private BigDecimal creditsUsedThisMonth;

    @Schema(description = "Credits used change percent compared to equivalent period in last month", example = "21.0")
    private BigDecimal creditsUsedChangeFromLastMonthPercent;
}