package com.fleebug.corerouter.dto.billing.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}
