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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "Usage Summary Response",
    description = "Aggregated usage and cost summary for a date range"
)
public class UsageSummaryResponse {

    @Schema(description = "Start of the queried period")
    private LocalDateTime periodStart;

    @Schema(description = "End of the queried period")
    private LocalDateTime periodEnd;

    @Schema(description = "Total cost across all usage in the period", example = "42.50")
    private BigDecimal totalCost;

    @Schema(description = "Breakdown by usage unit type or by model")
    private List<UsageSummaryItem> breakdown;
}
