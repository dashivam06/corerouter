package com.fleebug.corerouter.dto.otp;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@Schema(
    title = "Request OTP Response",
    description = "Response containing OTP verification details. Client should use verificationId for the next step in registration.",
    example = "{\"verificationId\": \"verify_abc123def456\", \"message\": \"OTP sent to email\", \"ttlMinutes\": 10}"
)
public class RequestOtpResponse {
    @Schema(
        description = "Unique verification identifier used in next step to verify OTP. Required for OTP verification request.",
        requiredMode = RequiredMode.REQUIRED,
        example = "verify_abc123def456"
    )
    private String verificationId;

    @Schema(
        description = "Confirmation message indicating OTP was sent successfully",
        requiredMode = RequiredMode.REQUIRED,
        example = "OTP sent to email"
    )
    private String message;

    @Schema(
        description = "Time-to-live in minutes. OTP expires after this duration. User must verify before expiration.",
        requiredMode = RequiredMode.REQUIRED,
        example = "10",
        minimum = "1",
        maximum = "60"
    )
    private int ttlMinutes;
}
