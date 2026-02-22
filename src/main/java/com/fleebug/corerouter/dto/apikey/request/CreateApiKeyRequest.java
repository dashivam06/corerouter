package com.fleebug.corerouter.dto.apikey.request;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "Create API Key Request",
    description = "Request to create a new API key with specified rate limits. Used to generate credentials for programmatic API access.",
    example = "{\"description\": \"Mobile App Integration\", \"dailyLimit\": 1000, \"monthlyLimit\": 25000}"
)
public class CreateApiKeyRequest {

    @NotBlank(message = "Description cannot be blank")
    @Schema(
        description = "A descriptive name for this API key (e.g., 'Production Server', 'Mobile App', 'Testing'). Helps identify the key's purpose.",
        requiredMode = RequiredMode.REQUIRED,
        example = "Mobile App Integration",
        minLength = 1,
        maxLength = 255
    )
    private String description;

    @Min(value = 1, message = "Daily limit must be at least 1")
    @Schema(
        description = "Maximum number of API requests allowed per day (UTC 00:00 to 23:59). Must be at least 1.",
        requiredMode = RequiredMode.REQUIRED,
        example = "1000",
        minimum = "1",
        maximum = "1000000"
    )
    private Integer dailyLimit;

    @Min(value = 1, message = "Monthly limit must be at least 1")
    @Schema(
        description = "Maximum number of API requests allowed per calendar month. Must be at least 1 and typically higher than daily limit.",
        requiredMode = RequiredMode.REQUIRED,
        example = "25000",
        minimum = "1",
        maximum = "10000000"
    )
    private Integer monthlyLimit;
}
