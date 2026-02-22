package com.fleebug.corerouter.dto.user.response;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "Authentication Response",
    description = "Contains authentication tokens and token expiration information returned after successful login or token refresh.",
    example = "{\"accessToken\": \"eyJhbGciOiJIUzI1NiIs...\", \"refreshToken\": \"eyJhbGciOiJIUzI1NiIs...\", \"expiresIn\": 3600}"
)
public class AuthResponse {

    @Schema(
        description = "JWT access token for authenticated API requests. Include in Authorization header as Bearer token.",
        requiredMode = RequiredMode.REQUIRED,
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
    )
    private String accessToken;

    @Schema(
        description = "JWT refresh token used to obtain a new access token without re-authentication. Store securely.",
        requiredMode = RequiredMode.REQUIRED,
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
    )
    private String refreshToken;

    @Schema(
        description = "Access token expiration time in seconds from the current time. After expiration, use refresh token to obtain a new access token.",
        requiredMode = RequiredMode.REQUIRED,
        example = "3600"
    )
    private Long expiresIn;
}
