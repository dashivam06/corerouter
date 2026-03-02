package com.fleebug.corerouter.dto.model.request;

import com.fleebug.corerouter.enums.model.ModelType;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "Create Model Request",
    description = "Request to create a new AI model or language model in the system. Contains all required information for model registration and pricing configuration.",
    example = "{\"fullname\": \"MISTRAL-7B API\", \"username\": \"mistral7b\", \"provider\": \"MISTRAL\", \"parameterCount\": \"175B\", \"pricePer1kTokens\": 0.03, \"endpointUrl\": \"https://api.mistral.com/v1/chat/completions\", \"description\": \"MISTRAL's most capable model\", \"type\": \"LLM\"}"
)
public class CreateModelRequest {

    @NotBlank(message = "Full name cannot be blank")
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    @Schema(
        description = "Display name of the model (e.g., 'MISTRAL-7B', 'Claude 3 Opus'). Should be user-friendly and descriptive.",
        requiredMode = RequiredMode.REQUIRED,
        example = "MISTRAL-7B API",
        minLength = 1,
        maxLength = 255
    )
    private String fullname;

    @NotBlank(message = "Username cannot be blank")
    @Size(max = 255, message = "Username must not exceed 255 characters")
    @Schema(
        description = "Unique identifier/slug for the model used in URLs and references (lowercase, no spaces). Example: 'mistral7b', 'claude3'.)",
        requiredMode = RequiredMode.REQUIRED,
        example = "mistral7b",
        minLength = 1,
        maxLength = 255,
        pattern = "^[a-z0-9_-]+$"
    )
    private String username;

    @NotBlank(message = "Provider cannot be blank")
    @Size(max = 255, message = "Provider must not exceed 255 characters")
    @Schema(
        description = "AI service provider (e.g., 'MISTRAL', 'Anthropic', 'Google', 'Meta'). Identifies the organization providing the model.",
        requiredMode = RequiredMode.REQUIRED,
        example = "MISTRAL",
        minLength = 1,
        maxLength = 255
    )
    private String provider;

    @NotBlank
    @Schema(description = "API endpoint URL", example = "https://api.mistral.com/v1/chat/completions")
    private String endpointUrl;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(
        description = "Detailed description of the model's capabilities, intended use cases, and special features.",
        example = "MISTRAL's most capable model with advanced reasoning and coding abilities",
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
