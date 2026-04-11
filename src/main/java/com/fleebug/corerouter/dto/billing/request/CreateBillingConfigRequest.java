package com.fleebug.corerouter.dto.billing.request;

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
    title = "Create Billing Config Request",
    description = "Request payload for creating a billing configuration for a model",
    example = "{\"modelId\": 1, \"pricingType\": \"PER_TOKEN\", \"pricingMetadata\": \"{\\\"inputRate\\\": 0.00003, \\\"outputRate\\\": 0.00006}\", \"chargeMultiplier\": 1.25}"
)
public class CreateBillingConfigRequest {

    @Schema(
        description = "ID of the model this billing config is for",
        requiredMode = RequiredMode.REQUIRED,
        example = "1",
        minimum = "1"
    )
    @NotNull(message = "modelId is required")
    private Integer modelId;

    @Schema(
        description = "Type of pricing (PER_TOKEN, PER_IMAGE, PER_REQUEST, etc.)",
        requiredMode = RequiredMode.REQUIRED,
        example = "PER_TOKEN"
    )
    @NotNull(message = "pricingType is required")
    private String pricingType;

    @Schema(
        description = "JSON metadata containing rate details specific to the pricing type. "
            + "For PER_TOKEN: {\"inputRate\": 0.00003, \"outputRate\": 0.00006}. "
            + "For PER_IMAGE: {\"rate\": 0.01}. "
            + "For PER_REQUEST: {\"rate\": 0.001}.",
        requiredMode = RequiredMode.REQUIRED,
        example = "{\"inputRate\": 0.00003, \"outputRate\": 0.00006}"
    )
    @NotNull(message = "pricingMetadata is required")
    private String pricingMetadata;

    @Schema(
        description = "Multiplier applied to computed task cost when charging wallet. 1.0 means no markup; 3.0 means user is charged 3x.",
        requiredMode = RequiredMode.NOT_REQUIRED,
        example = "1.25",
        defaultValue = "1.0",
        minimum = "0.0001"
    )
    @DecimalMin(value = "0.0001", message = "chargeMultiplier must be greater than 0")
    private BigDecimal chargeMultiplier;
}
