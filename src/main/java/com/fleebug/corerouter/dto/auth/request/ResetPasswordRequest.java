package com.fleebug.corerouter.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
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
    title = "Reset Password Request",
    description = "Final step for forgot-password flow after OTP verification.",
    example = "{\"verificationId\":\"verify_abc123\",\"newPassword\":\"NewP@ss123\",\"confirmPassword\":\"NewP@ss123\"}"
)
public class ResetPasswordRequest {

    @NotBlank(message = "VerificationId is required")
    @Schema(
        description = "Verification ID from OTP request and verification steps",
        requiredMode = RequiredMode.REQUIRED,
        example = "verify_abc123"
    )
    private String verificationId;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 255, message = "New password must be between 8 and 255 characters")
    @Schema(
        description = "New password to set",
        requiredMode = RequiredMode.REQUIRED,
        example = "NewP@ss123",
        minLength = 8,
        maxLength = 255
    )
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    @Size(min = 8, max = 255, message = "Confirm password must be between 8 and 255 characters")
    @Schema(
        description = "Must match newPassword",
        requiredMode = RequiredMode.REQUIRED,
        example = "NewP@ss123",
        minLength = 8,
        maxLength = 255
    )
    private String confirmPassword;
}
