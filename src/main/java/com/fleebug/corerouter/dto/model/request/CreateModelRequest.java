package com.fleebug.corerouter.dto.model.request;

import com.fleebug.corerouter.enums.model.ModelType;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.*;
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
    title = "Create Model Request",
    description = "Request to create a new AI model or language model in the system. Contains all required information for model registration and pricing configuration.",
    example = "{\"fullname\": \"GPT-4 API\", \"username\": \"gpt4\", \"provider\": \"OpenAI\", \"parameterCount\": \"175B\", \"pricePer1kTokens\": 0.03, \"endpointUrl\": \"https://api.openai.com/v1/chat/completions\", \"description\": \"OpenAI's most capable model\", \"type\": \"LLM\"}"
)
public class CreateModelRequest {

    @NotBlank(message = "Full name cannot be blank")
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    @Schema(
        description = "Display name of the model (e.g., 'GPT-4', 'Claude 3 Opus'). Should be user-friendly and descriptive.",
        requiredMode = RequiredMode.REQUIRED,
        example = "GPT-4 API",
        minLength = 1,
        maxLength = 255
    )
    private String fullname;

    @NotBlank(message = "Username cannot be blank")
    @Size(max = 255, message = "Username must not exceed 255 characters")
    @Schema(
        description = "Unique identifier/slug for the model used in URLs and references (lowercase, no spaces). Example: 'gpt4', 'claude3'.)",
        requiredMode = RequiredMode.REQUIRED,
        example = "gpt4",
        minLength = 1,
        maxLength = 255,
        pattern = "^[a-z0-9_-]+$"
    )
    private String username;

    @NotBlank(message = "Provider cannot be blank")
    @Size(max = 255, message = "Provider must not exceed 255 characters")
    @Schema(
        description = "AI service provider (e.g., 'OpenAI', 'Anthropic', 'Google', 'Meta'). Identifies the organization providing the model.",
        requiredMode = RequiredMode.REQUIRED,
        example = "OpenAI",
        minLength = 1,
        maxLength = 255
    )
    private String provider;

    @NotBlank(message = "Parameter count cannot be blank")
    @Schema(
        description = "Total number of parameters in the model (e.g., '175B', '70B', '13B'). Can include unit suffix (B for billion, M for million).",
        requiredMode = RequiredMode.REQUIRED,
        example = "175B",
        maxLength = 50
    )
    private String parameterCount;

    @NotNull(message = "Price per 1k tokens is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Schema(
        description = "Cost in USD per 1000 tokens (prompt + completion combined). Determines billing for API usage.",
        requiredMode = RequiredMode.REQUIRED,
        example = "0.03",
        minimum = "0.0001",
        maximum = "999.99"
    )
    private BigDecimal pricePer1kTokens;

    @NotBlank(message = "Endpoint URL cannot be blank")
    @Schema(
        description = "The API endpoint URL where requests to this model will be routed. Should be the base URL for the model's API.",
        requiredMode = RequiredMode.REQUIRED,
        example = "https://api.openai.com/v1/chat/completions",
        format = "uri"
    )
    private String endpointUrl;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(
        description = "Detailed description of the model's capabilities, intended use cases, and special features.",
        example = "OpenAI's most capable model with advanced reasoning and coding abilities",
        maxLength = 500
    )
    private String description;

    @NotNull(message = "Type cannot be null")
    @Schema(
        description = "Classification of the model type (LLM, Vision, Embedding, FineTuning, etc.). Determines the model's primary function.",
        requiredMode = RequiredMode.REQUIRED,
        example = "LLM",
        enumAsRef = true
    )
    private ModelType type;
}
