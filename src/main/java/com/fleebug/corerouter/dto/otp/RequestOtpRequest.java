package com.fleebug.corerouter.dto.otp;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(
    title = "Request OTP Request",
    description = "Request to send a one-time password (OTP) to the specified email address. Initiated during registration process.",
    example = "{\"email\": \"john.doe@example.com\"}"
)
public class RequestOtpRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(
        description = "The email address where the OTP will be sent. Must be a valid, non-registered email address.",
        requiredMode = RequiredMode.REQUIRED,
        example = "john.doe@example.com",
        format = "email"
    )
    private String email;
}
