package com.fleebug.corerouter.dto.apikey.response;

import java.time.LocalDateTime;
import com.fleebug.corerouter.enums.apikey.ApiKeyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "API Key Response",
    description = "Complete details of an API key including credentials, limits, and usage tracking information.",
    example = "{\"apiKeyId\": 123, \"key\": \"pk_live_xxxxxxxxxxxxx\", \"description\": \"Production Server\", \"dailyLimit\": 1000, \"monthlyLimit\": 25000, \"status\": \"ACTIVE\", \"createdAt\": \"2024-01-15T10:30:00\", \"lastUsedAt\": \"2024-02-22T16:45:00\"}"
)
public class ApiKeyResponse {

    @Schema(
        description = "Unique identifier for the API key in database",
        requiredMode = RequiredMode.REQUIRED,
        example = "123"
    )
    private Integer apiKeyId;

    @Schema(
        description = "The actual API key credential. Keep this secret and use it to authenticate API requests. Store securely.",
        requiredMode = RequiredMode.REQUIRED,
        example = "pk_live_51234567890abcdefghijklmnop",
        pattern = "^[a-zA-Z0-9_-]{20,}$"
    )
    private String key;

    @Schema(
        description = "A user-defined description that identifies the purpose or location of this API key",
        example = "Production Server",
        maxLength = 255
    )
    private String description;

    @Schema(
        description = "Maximum number of API requests allowed per calendar day (UTC)",
        requiredMode = RequiredMode.REQUIRED,
        example = "1000",
        minimum = "1"
    )
    private Integer dailyLimit;

    @Schema(
        description = "Maximum number of API requests allowed per calendar month",
        requiredMode = RequiredMode.REQUIRED,
        example = "25000",
        minimum = "1"
    )
    private Integer monthlyLimit;

    @Schema(
        description = "Current status of the API key (ACTIVE, INACTIVE, REVOKED, EXPIRED). Only ACTIVE keys can be used for API requests.",
        requiredMode = RequiredMode.REQUIRED,
        example = "ACTIVE",
        enumAsRef = true
    )
    private ApiKeyStatus status;

    @Schema(
        description = "Timestamp when this API key was created",
        requiredMode = RequiredMode.REQUIRED,
        example = "2024-01-15T10:30:00Z",
        format = "date-time"
    )
    private LocalDateTime createdAt;

    @Schema(
        description = "Timestamp of the last request made using this API key. Null if never used.",
        example = "2024-02-22T16:45:00Z",
        format = "date-time"
    )
    private LocalDateTime lastUsedAt;
}
