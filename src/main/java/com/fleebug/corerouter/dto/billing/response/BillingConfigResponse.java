package com.fleebug.corerouter.dto.billing.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "Billing Config Response",
    description = "Response payload representing a billing configuration for a model"
)
public class BillingConfigResponse {

    @Schema(description = "Billing config ID", example = "1")
    private Integer billingId;

    @Schema(description = "Model ID this config is for", example = "5")
    private Integer modelId;

    @Schema(description = "Model name", example = "mistral-7B")
    private String modelName;

    @Schema(description = "Pricing type", example = "PER_TOKEN")
    private String pricingType;

    @Schema(
        description = "JSON pricing metadata with rate details",
        example = "{\"inputRate\": 0.00003, \"outputRate\": 0.00006}"
    )
    private String pricingMetadata;

    @Schema(description = "When this config was created")
    private LocalDateTime createdAt;

    @Schema(description = "When this config was last updated")
    private LocalDateTime updatedAt;
}
