package com.fleebug.corerouter.dto.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
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
    title = "Update Provider Request",
    description = "Request to update an existing AI model provider.",
    example = "{\"providerName\": \"Mistral AI Inc\", \"providerCountry\": \"France\", \"companyName\": \"Mistral AI SAS Updated\", \"logo\": \"https://cdn.example.com/logos/mistral-new.png\"}"
)
public class UpdateProviderRequest {

    @Size(max = 255, message = "Provider name must not exceed 255 characters")
    @Schema(
        description = "Name of the AI provider (optional)",
        requiredMode = RequiredMode.NOT_REQUIRED,
        example = "Mistral AI Inc",
        maxLength = 255
    )
    private String providerName;

    @Size(max = 255, message = "Provider country must not exceed 255 characters")
    @Schema(
        description = "Country where the provider is based (optional)",
        requiredMode = RequiredMode.NOT_REQUIRED,
        example = "France",
        maxLength = 255
    )
    private String providerCountry;

    @Size(max = 255, message = "Company name must not exceed 255 characters")
    @Schema(
        description = "Official company name of the provider (optional)",
        requiredMode = RequiredMode.NOT_REQUIRED,
        example = "Mistral AI SAS Updated",
        maxLength = 255
    )
    private String companyName;

    @Schema(
        description = "Public image URL for provider logo (optional)",
        requiredMode = RequiredMode.NOT_REQUIRED,
        example = "https://cdn.example.com/logos/mistral-new.png"
    )
    private String logo;
}
