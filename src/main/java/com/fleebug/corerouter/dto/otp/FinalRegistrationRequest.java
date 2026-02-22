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
    title = "Final Registration Request",
    description = "Final step of user registration. Completes profile creation after OTP verification. Creates the user account with provided details.",
    example = "{\"verificationId\": \"verify_abc123def456\", \"fullName\": \"John Doe\", \"password\": \"SecureP@ss123\", \"confirmPassword\": \"SecureP@ss123\", \"profileImage\": \"https://example.com/avatar.jpg\", \"emailSubscribed\": true}"
)
public class FinalRegistrationRequest {
    @NotBlank(message = "VerificationId is required")
    @Schema(
        description = "Verification token from OTP verification step. Proves user has verified their email.",
        requiredMode = RequiredMode.REQUIRED,
        example = "verify_abc123def456"
    )
    private String verificationId;

    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
    @Schema(
        description = "User's full name (first and last name). Used for display and profile.",
        requiredMode = RequiredMode.REQUIRED,
        example = "John Doe",
        minLength = 3,
        maxLength = 100
    )
    private String fullName;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(
        description = "User's new account password. Must be at least 8 characters with mixed case, numbers, and special characters recommended.",
        requiredMode = RequiredMode.REQUIRED,
        example = "SecureP@ss123",
        minLength = 8,
        maxLength = 255,
        pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$"
    )
    private String password;

    @NotBlank(message = "Confirm password is required")
    @Schema(
        description = "Password confirmation. Must exactly match the password field to prevent typos.",
        requiredMode = RequiredMode.REQUIRED,
        example = "SecureP@ss123",
        minLength = 8,
        maxLength = 255
    )
    private String confirmPassword;

    @Schema(
        description = "Optional URL to user's profile image/avatar. Can be a local or external image URL.",
        example = "https://example.com/avatar.jpg",
        format = "uri"
    )
    private String profileImage;

    @Schema(
        description = "Boolean flag indicating whether user wants to subscribe to email newsletters and product updates.",
        required = false,
        example = "true"
    )
    private boolean emailSubscribed;
}
