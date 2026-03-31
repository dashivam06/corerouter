package com.fleebug.corerouter.dto.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "Create Provider Request",
    description = "Request to create a new AI model provider in the system.",
    example = "{\"providerName\": \"Mistral AI\", \"providerCountry\": \"France\", \"companyName\": \"Mistral AI SAS\", \"logo\": \"https://cdn.example.com/logos/mistral.png\"}"
)
public class CreateProviderRequest {

    @NotBlank(message = "Provider name cannot be blank")
    @Size(max = 255, message = "Provider name must not exceed 255 characters")
    @Schema(
        description = "Name of the AI provider (e.g., 'Mistral AI', 'Anthropic', 'OpenAI')",
        requiredMode = RequiredMode.REQUIRED,
        example = "Mistral AI",
        minLength = 1,
        maxLength = 255
    )
    private String providerName;

    @NotBlank(message = "Provider country cannot be blank")
    @Size(max = 255, message = "Provider country must not exceed 255 characters")
    @Schema(
        description = "Country where the provider is based (e.g., 'France', 'United States', 'UK')",
        requiredMode = RequiredMode.REQUIRED,
        example = "France",
        minLength = 1,
        maxLength = 255
    )
    private String providerCountry;

    @NotBlank(message = "Company name cannot be blank")
    @Size(max = 255, message = "Company name must not exceed 255 characters")
    @Schema(
        description = "Official company name of the provider",
        requiredMode = RequiredMode.REQUIRED,
        example = "Mistral AI SAS",
        minLength = 1,
        maxLength = 255
    )
    private String companyName;

    @Schema(
        description = "Public image URL for provider logo (optional)",
        requiredMode = RequiredMode.NOT_REQUIRED,
        example = "https://cdn.example.com/logos/mistral.png"
    )
    private String logo;
}
