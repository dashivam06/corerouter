package com.fleebug.corerouter.dto.otp;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal cache data structure for storing pending registration with OTP.
 * Not exposed via API - used internally for Redis caching during registration flow.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "OTP Cache Data",
    description = "Internal data structure for caching OTP verification data during registration. Stored in Redis, not exposed in API responses.",
    hidden = true
)
public class OtpCacheData {

    @Schema(
        description = "Email address associated with this OTP verification session",
        requiredMode = RequiredMode.REQUIRED,
        example = "john.doe@example.com",
        format = "email"
    )
    private String email;

    @JsonProperty("registration_data")
    @Schema(
        description = "Serialized registration data pending verification. Stored as JSON string containing user information.",
        requiredMode = RequiredMode.REQUIRED,
        example = "{\"fullName\": \"John Doe\", \"emailSubscribed\": true}"
    )
    private String registrationData;

    @JsonProperty("created_at")
    @Schema(
        description = "Unix timestamp (milliseconds) when this cache entry was created. Used for TTL calculation.",
        requiredMode = RequiredMode.REQUIRED,
        example = "1708601400000",
        format = "int64"
    )
    private long createdAt;
}
