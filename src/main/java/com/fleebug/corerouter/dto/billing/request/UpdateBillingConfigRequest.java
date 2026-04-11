package com.fleebug.corerouter.dto.billing.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
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
    title = "Update Billing Config Request",
    description = "Request payload for updating an existing billing configuration"
)
public class UpdateBillingConfigRequest {

    @Schema(
        description = "Updated pricing type",
        example = "PER_TOKEN"
    )
    private String pricingType;

    @Schema(
        description = "Updated JSON pricing metadata",
        example = "{\"inputRate\": 0.00005, \"outputRate\": 0.00010}"
    )
    private String pricingMetadata;

    @Schema(
        description = "Updated multiplier applied on computed task cost for wallet charging",
        example = "1.50",
        minimum = "0.0001"
    )
    @DecimalMin(value = "0.0001", message = "chargeMultiplier must be greater than 0")
    private BigDecimal chargeMultiplier;
}
