package com.fleebug.corerouter.dto.billing.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(title = "Billing Insights Response", description = "High-level billing insights for dashboard cards")
public class BillingInsightsResponse {

    @Schema(description = "Current wallet balance", example = "1250.50")
    private BigDecimal totalBalance;

    @Schema(description = "Total billing volume for the current month in NPR", example = "842.75")
    private BigDecimal thisMonthVolume;

    @Schema(description = "Total successful top-up amount for today in NPR", example = "500.00")
    private BigDecimal todayTopUpAmount;
}
