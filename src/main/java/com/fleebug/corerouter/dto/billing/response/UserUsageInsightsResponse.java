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
@Schema(title = "User Usage Insights Response", description = "Dashboard insights for authenticated user's usage page")
public class UserUsageInsightsResponse {

    @Schema(description = "Total spend for current period", example = "0.07")
    private BigDecimal totalSpend;

    @Schema(description = "Total spend change percent versus prior period", example = "12.0")
    private BigDecimal totalSpendChangePercent;

    @Schema(description = "Total requests for current period", example = "4")
    private Long totalRequests;

    @Schema(description = "Total requests change percent versus prior period", example = "4.0")
    private BigDecimal totalRequestsChangePercent;

    @Schema(description = "Most used model in current period", example = "GPT-4o November 2024")
    private String mostUsedModel;

    @Schema(description = "Average cost per request for current period", example = "0.02")
    private BigDecimal avgCostPerRequest;

    @Schema(description = "Average cost per request change percent versus prior period", example = "-3.0")
    private BigDecimal avgCostPerRequestChangePercent;
}