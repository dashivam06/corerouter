package com.fleebug.corerouter.dto.model.response;

import com.fleebug.corerouter.enums.model.ProviderStatus;
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
    title = "Provider Response",
    description = "Contains detailed information about an AI model provider.",
    example = "{\"providerId\": 1, \"providerName\": \"Mistral AI\", \"providerCountry\": \"France\", \"companyName\": \"Mistral AI SAS\", \"status\": \"ACTIVE\", \"createdAt\": \"2024-01-15T10:30:00Z\", \"updatedAt\": \"2024-02-22T14:20:00Z\"}"
)
public class ProviderResponse {

    @Schema(
        description = "Unique database identifier for the provider",
        requiredMode = RequiredMode.REQUIRED,
        example = "1"
    )
    private Integer providerId;

    @Schema(
        description = "Name of the AI provider",
        requiredMode = RequiredMode.REQUIRED,
        example = "Mistral AI",
        maxLength = 255
    )
    private String providerName;

    @Schema(
        description = "Country where the provider is based",
        requiredMode = RequiredMode.REQUIRED,
        example = "France",
        maxLength = 255
    )
    private String providerCountry;

    @Schema(
        description = "Official company name",
        requiredMode = RequiredMode.REQUIRED,
        example = "Mistral AI SAS",
        maxLength = 255
    )
    private String companyName;

    @Schema(
        description = "Provider status (ACTIVE, DISABLED, SUSPENDED, DELETED)",
        requiredMode = RequiredMode.REQUIRED,
        example = "ACTIVE"
    )
    private ProviderStatus status;

    @Schema(
        description = "Provider logo URL (optional)",
        requiredMode = RequiredMode.NOT_REQUIRED,
        example = "https://cdn.example.com/logos/mistral.png"
    )
    private String logo;

    @Schema(
        description = "Timestamp when the provider was created",
        requiredMode = RequiredMode.REQUIRED,
        example = "2024-01-15T10:30:00"
    )
    private LocalDateTime createdAt;

    @Schema(
        description = "Timestamp when the provider was last updated",
        requiredMode = RequiredMode.REQUIRED,
        example = "2024-02-22T14:20:00"
    )
    private LocalDateTime updatedAt;
}
