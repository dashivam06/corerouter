package com.fleebug.corerouter.dto.apikey.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
    title = "Update API Key Request",
    description = "Request to update existing API key settings. All fields are optional; only provided fields will be updated.",
    example = "{\"description\": \"Updated Production Key\", \"dailyLimit\": 2000, \"monthlyLimit\": 50000}"
)
public class UpdateApiKeyRequest {

    @Schema(
        description = "Updated description for the API key. Leave null to keep existing value.",
        example = "Updated Production Key",
        minLength = 1,
        maxLength = 255
    )
    private String description;

    @Min(value = 1, message = "Daily limit must be at least 1")
    @Schema(
        description = "Updated daily request limit. Leave null to keep existing value. Must be at least 1 if provided.",
        example = "2000",
        minimum = "1",
        maximum = "1000000"
    )
    private Integer dailyLimit;

    @Min(value = 1, message = "Monthly limit must be at least 1")
    @Schema(
        description = "Updated monthly request limit. Leave null to keep existing value. Must be at least 1 if provided.",
        example = "50000",
        minimum = "1",
        maximum = "10000000"
    )
    private Integer monthlyLimit;
}
