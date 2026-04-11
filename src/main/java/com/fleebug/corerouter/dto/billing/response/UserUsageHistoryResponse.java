package com.fleebug.corerouter.dto.billing.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "User Usage History Response", description = "Daily usage history grouped by usage unit type")
public class UserUsageHistoryResponse {

    @Schema(description = "Applied period label", example = "30days")
    private String period;

    @Schema(description = "Applied start date-time")
    private LocalDateTime fromDate;

    @Schema(description = "Applied end date-time")
    private LocalDateTime toDate;

    @Schema(description = "Daily usage history")
    private List<DailyUsageHistoryDay> dailyHistory;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyUsageHistoryDay {

        @Schema(description = "Date label", example = "2026-01-01")
        private String date;

        @Schema(description = "Total usage cost for the date", example = "0.021")
        private BigDecimal totalCost;

        @Schema(description = "Distinct request count for the date", example = "4")
        private Long totalRequests;

        @Schema(description = "Usage unit breakdown by unit name")
        private Map<String, UnitUsageSummary> usageByUnit;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnitUsageSummary {

        @Schema(description = "Total quantity for the usage unit on the date", example = "10")
        private BigDecimal quantity;

        @Schema(description = "Total cost for the usage unit on the date", example = "0.010")
        private BigDecimal totalCost;

        @Schema(description = "Average effective rate for the usage unit on the date", example = "0.001000")
        private BigDecimal avgRatePerUnit;
    }
}
