package com.fleebug.corerouter.dto.model.response;

import com.fleebug.corerouter.enums.model.ModelStatus;
import com.fleebug.corerouter.enums.model.ModelType;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "Model Response",
    description = "Contains detailed information about a single AI model including configuration, pricing, and metadata.",
    example = "{\"modelId\": 1, \"fullname\": \"MISTRAL-7B\", \"username\": \"mistral7b\", \"provider\": \"MISTRAL\", \"parameterCount\": \"175B\", \"pricePer1kTokens\": 0.03, \"status\": \"ACTIVE\", \"endpointUrl\": \"https://api.mistral.com/v1/chat/completions\", \"type\": \"LLM\", \"description\": \"Most capable model\", \"createdAt\": \"2024-01-15T10:30:00Z\", \"updatedAt\": \"2024-02-22T14:20:00Z\"}"
)
public class ModelResponse {

    @Schema(
        description = "Unique database identifier for the model",
        requiredMode = RequiredMode.REQUIRED,
        example = "1"
    )
    private Integer modelId;

    @Schema(
        description = "Display name of the model",
        requiredMode = RequiredMode.REQUIRED,
        example = "MISTRAL-7B",
        maxLength = 255
    )
    private String fullname;

    @Schema(
        description = "Unique identifier/slug for the model used in URLs",
        requiredMode = RequiredMode.REQUIRED,
        example = "mistral7b",
        maxLength = 255
    )
    private String username;

    @Schema(
        description = "AI service provider name",
        requiredMode = RequiredMode.REQUIRED,
        example = "MISTRAL",
        maxLength = 255
    )
    private String provider;

    @Schema(description = "Current status", example = "ACTIVE")
    private ModelStatus status;

    @Schema(
        description = "API endpoint URL for the model",
        requiredMode = RequiredMode.REQUIRED,
        example = "https://api.mistral.com/v1/chat/completions",
        format = "uri"
    )
    private String endpointUrl;

    @Schema(
        description = "Type/classification of the model",
        requiredMode = RequiredMode.REQUIRED,
        example = "LLM",
        enumAsRef = true
    )
    private ModelType type;

    @Schema(
        description = "Description of model capabilities and features",
        example = "MISTRAL's most capable model with advanced reasoning abilities",
        maxLength = 500
    )
    private String description;

    @Schema(
        description = "Timestamp when the model was registered",
        requiredMode = RequiredMode.REQUIRED,
        example = "2024-01-15T10:30:00Z",
        format = "date-time"
    )
    private LocalDateTime createdAt;

    @Schema(
        description = "Timestamp of the last update to model information",
        requiredMode = RequiredMode.REQUIRED,
        example = "2024-02-22T14:20:00Z",
        format = "date-time"
    )
    private LocalDateTime updatedAt;
}
