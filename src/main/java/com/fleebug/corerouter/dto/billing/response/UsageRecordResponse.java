package com.fleebug.corerouter.dto.billing.response;

import com.fleebug.corerouter.enums.billing.UsageUnitType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "Usage Record Response",
    description = "Response payload representing a single usage record"
)
public class UsageRecordResponse {

    @Schema(description = "Usage record ID", example = "100")
    private Long usageId;

    @Schema(description = "Task ID this usage belongs to", example = "abc-123-def-456")
    private String taskId;

    @Schema(description = "Model ID used", example = "5")
    private Integer modelId;

    @Schema(description = "Model name", example = "mistral-7B")
    private String modelName;

    @Schema(description = "API key ID", example = "10")
    private Integer apiKeyId;

    @Schema(description = "Type of usage unit", example = "TOKENS")
    private UsageUnitType usageUnitType;

    @Schema(description = "Quantity consumed", example = "1500")
    private BigDecimal quantity;

    @Schema(description = "Rate per unit at recording time", example = "0.00003")
    private BigDecimal ratePerUnit;

    @Schema(description = "Total cost for this record", example = "0.045")
    private BigDecimal cost;

    @Schema(description = "When usage was recorded")
    private LocalDateTime recordedAt;
}
