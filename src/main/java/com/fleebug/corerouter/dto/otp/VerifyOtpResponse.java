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
    title = "Verify OTP Response",
    description = "Response confirming OTP verification result. If verified, contains a token for completing registration.",
    example = "{\"verificationId\": \"verify_abc123def456\", \"message\": \"OTP verified successfully\", \"verified\": true, \"profileCompletionTtlMinutes\": 30}"
)
public class VerifyOtpResponse {
    @Schema(
        description = "The same verification ID. Used to link OTP verification with the final registration step.",
        requiredMode = RequiredMode.REQUIRED,
        example = "verify_abc123def456"
    )
    private String verificationId;

    @Schema(
        description = "Status message describing the verification result",
        requiredMode = RequiredMode.REQUIRED,
        example = "OTP verified successfully"
    )
    private String message;

    @Schema(
        description = "Boolean flag indicating whether OTP verification was successful",
        requiredMode = RequiredMode.REQUIRED,
        example = "true"
    )
    private boolean verified;

    @Schema(
        description = "Time-to-live in minutes for completing profile setup. User must submit final registration before expiration.",
        requiredMode = RequiredMode.REQUIRED,
        example = "30",
        minimum = "1",
        maximum = "120"
    )
    private int profileCompletionTtlMinutes;
}
