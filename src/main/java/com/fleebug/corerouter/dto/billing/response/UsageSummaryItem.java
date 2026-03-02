package com.fleebug.corerouter.dto.billing.response;

import com.fleebug.corerouter.enums.billing.UsageUnitType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "Usage Summary Item",
    description = "A single row in a usage summary breakdown"
)
public class UsageSummaryItem {

    @Schema(description = "Model ID (present in per-model breakdowns)", example = "5")
    private Integer modelId;

    @Schema(description = "Model name (present in per-model breakdowns)", example = "mistral-7B")
    private String modelName;

    @Schema(description = "Usage unit type", example = "TOKENS")
    private UsageUnitType usageUnitType;

    @Schema(description = "Total quantity consumed", example = "150000")
    private BigDecimal totalQuantity;

    @Schema(description = "Total cost", example = "4.50")
    private BigDecimal totalCost;

    @Schema(description = "Number of tasks", example = "120")
    private Long taskCount;
}
