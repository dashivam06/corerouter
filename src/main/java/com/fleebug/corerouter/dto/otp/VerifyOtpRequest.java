package com.fleebug.corerouter.dto.otp;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(
    title = "Verify OTP Request",
    description = "Request to verify the one-time password sent to user's email. Completes the first step of the authentication process.",
    example = "{\"verificationId\": \"verify_abc123def456\", \"otp\": \"123456\"}"
)
public class VerifyOtpRequest {
    @NotBlank(message = "VerificationId is required")
    @Schema(
        description = "The verification ID obtained from the RequestOtpResponse. Ties OTP verification to the initial OTP request.",
        requiredMode = RequiredMode.REQUIRED,
        example = "verify_abc123def456"
    )
    private String verificationId;

    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP must be 6 digits")
    @Schema(
        description = "The six-digit OTP code sent to the user's email. Must match exactly including all digits.",
        requiredMode = RequiredMode.REQUIRED,
        example = "123456",
        minLength = 6,
        maxLength = 6,
        pattern = "^\\d{6}$"
    )
    private String otp;
}
