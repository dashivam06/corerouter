package com.fleebug.corerouter.dto.model.response;

import com.fleebug.corerouter.enums.model.ModelStatus;
import com.fleebug.corerouter.enums.model.ModelType;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "Model Response",
    description = "Contains detailed information about a single AI model including configuration, pricing, and metadata.",
    example = "{\"modelId\": 1, \"fullname\": \"GPT-4\", \"username\": \"gpt4\", \"provider\": \"OpenAI\", \"parameterCount\": \"175B\", \"pricePer1kTokens\": 0.03, \"status\": \"ACTIVE\", \"endpointUrl\": \"https://api.openai.com/v1/chat/completions\", \"type\": \"LLM\", \"description\": \"Most capable model\", \"createdAt\": \"2024-01-15T10:30:00Z\", \"updatedAt\": \"2024-02-22T14:20:00Z\"}"
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
        example = "GPT-4",
        maxLength = 255
    )
    private String fullname;

    @Schema(
        description = "Unique identifier/slug for the model used in URLs",
        requiredMode = RequiredMode.REQUIRED,
        example = "gpt4",
        maxLength = 255
    )
    private String username;

    @Schema(
        description = "AI service provider name",
        requiredMode = RequiredMode.REQUIRED,
        example = "OpenAI",
        maxLength = 255
    )
    private String provider;

    @Schema(
        description = "Total number of model parameters",
        example = "175B",
        maxLength = 50
    )
    private String parameterCount;

    @Schema(
        description = "Pricing in USD per 1000 tokens",
        requiredMode = RequiredMode.REQUIRED,
        example = "0.03",
        minimum = "0.0001",
        maximum = "999.99"
    )
    private BigDecimal pricePer1kTokens;

    @Schema(
        description = "Current status of the model (ACTIVE, INACTIVE, DEPRECATED)",
        requiredMode = RequiredMode.REQUIRED,
        example = "ACTIVE",
        enumAsRef = true
    )
    private ModelStatus status;

    @Schema(
        description = "API endpoint URL for the model",
        requiredMode = RequiredMode.REQUIRED,
        example = "https://api.openai.com/v1/chat/completions",
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
        example = "OpenAI's most capable model with advanced reasoning abilities",
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
