package com.fleebug.corerouter.dto.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
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
    title = "Refresh Token Request",
    description = "Request payload to refresh authentication tokens. Provides a valid refresh token to obtain a new access token.",
    example = "{\"refreshToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\"}"
)
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token cannot be blank")
    @Schema(
        description = "The refresh token obtained from initial login. Used to obtain a new access token without re-entering credentials.",
        requiredMode = RequiredMode.REQUIRED,
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private String refreshToken;
}
