package com.fleebug.corerouter.dto.model.request;

import com.fleebug.corerouter.enums.model.ModelStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "Update Model Request",
    description = "Request to update existing model properties. All fields are optional; only provided fields will be updated.",
    example = "{\"fullname\": \"GPT-4 Turbo\", \"pricePer1kTokens\": 0.02, \"description\": \"Updated with better performance\"}"
)
public class UpdateModelRequest {

    @Schema(
        description = "Updated display name for the model. Leave null to keep existing value.",
        example = "GPT-4 Turbo",
        maxLength = 255
    )
    private String fullname;

    @Schema(
        description = "Updated unique identifier/slug for the model. Leave null to keep existing value.",
        example = "gpt4-turbo",
        maxLength = 255,
        pattern = "^[a-z0-9_-]+$"
    )
    private String username;

    @Schema(
        description = "Updated provider name. Leave null to keep existing value.",
        example = "OpenAI",
        maxLength = 255
    )
    private String provider;

    @Schema(
        description = "Updated parameter count. Leave null to keep existing value.",
        example = "175B",
        maxLength = 50
    )
    private String parameterCount;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Schema(
        description = "Updated price per 1000 tokens in USD. Leave null to keep existing value. Must be greater than 0 if provided.",
        example = "0.02",
        minimum = "0.0001",
        maximum = "999.99"
    )
    private BigDecimal pricePer1kTokens;

    @Schema(
        description = "Updated API endpoint URL. Leave null to keep existing value.",
        example = "https://api.openai.com/v1/chat/completions",
        format = "uri"
    )
    private String endpointUrl;

    @Schema(
        description = "Updated model description and capabilities. Leave null to keep existing value.",
        example = "Updated with better performance and lower latency",
        maxLength = 500
    )
    private String description;

    @Schema(
        description = "Updated model status (ACTIVE, INACTIVE, DEPRECATED). Controls whether the model is available for use.",
        example = "ACTIVE",
        enumAsRef = true
    )
    private ModelStatus status;
}
