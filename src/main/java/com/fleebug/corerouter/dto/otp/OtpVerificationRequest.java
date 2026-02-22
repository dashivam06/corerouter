package com.fleebug.corerouter.dto.otp;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "OTP Verification Request",
    description = "Alternative OTP verification request combining email and OTP code in one request (if using simpler flow).",
    example = "{\"email\": \"john.doe@example.com\", \"otp\": \"123456\"}"
)
public class OtpVerificationRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(
        description = "The email address the OTP was sent to",
        requiredMode = RequiredMode.REQUIRED,
        example = "john.doe@example.com",
        format = "email"
    )
    private String email;

    @NotBlank(message = "OTP is required")
    @Schema(
        description = "The one-time password code received via email",
        requiredMode = RequiredMode.REQUIRED,
        example = "123456",
        minLength = 6,
        maxLength = 6
    )
    private String otp;
}
