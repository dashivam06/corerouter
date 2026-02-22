package com.fleebug.corerouter.dto.user.request;

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
    title = "Login Request",
    description = "Request payload for user authentication. Validates email format and ensures both fields are provided.",
    example = "{\"email\": \"john.doe@example.com\", \"password\": \"P@ssword123\"}"
)
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(
        description = "The user's registered email address used for login",
        example = "john.doe@example.com",
        requiredMode = RequiredMode.REQUIRED,
        minLength = 5,
        maxLength = 255
    )
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(
        description = "The user's plain-text password (will be encrypted on server)",
        example = "P@ssword123",
        requiredMode = RequiredMode.REQUIRED,
        minLength = 8,
        maxLength = 255
    )
    private String password;
}
