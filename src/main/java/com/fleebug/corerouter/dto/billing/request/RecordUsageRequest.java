package com.fleebug.corerouter.dto.billing.request;

import com.fleebug.corerouter.enums.billing.UsageUnitType;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
    title = "Record Usage Request",
    description = "Request payload for recording usage against a completed task. "
        + "A single task may produce multiple usage records (e.g., input tokens + output tokens).",
    example = "{\"taskId\": \"abc-123\", \"usageUnitType\": \"TOKENS\", \"quantity\": 1500}"
)
public class RecordUsageRequest {

    @Schema(
        description = "Task ID the usage belongs to",
        requiredMode = RequiredMode.REQUIRED,
        example = "abc-123-def-456"
    )
    @NotNull(message = "taskId is required")
    private String taskId;

    @Schema(
        description = "Unit type being recorded (TOKENS, IMAGES, SECONDS, REQUESTS, etc.)",
        requiredMode = RequiredMode.REQUIRED,
        example = "TOKENS"
    )
    @NotNull(message = "usageUnitType is required")
    private UsageUnitType usageUnitType;

    @Schema(
        description = "Quantity of units consumed",
        requiredMode = RequiredMode.REQUIRED,
        example = "1500",
        minimum = "0.0001"
    )
    @NotNull(message = "quantity is required")
    @DecimalMin(value = "0.0001", message = "quantity must be positive")
    private BigDecimal quantity;
}
