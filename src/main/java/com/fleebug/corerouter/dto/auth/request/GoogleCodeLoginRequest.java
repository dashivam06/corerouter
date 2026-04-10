package com.fleebug.corerouter.dto.auth.request;

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
    title = "Google Code Login Request",
    description = "Request payload containing Google OAuth authorization code for backend token exchange.",
    example = "{\"code\":\"4/0AbCdef...\"}"
)
public class GoogleCodeLoginRequest {

    @NotBlank(message = "Authorization code is required")
    @Schema(
        description = "Google OAuth authorization code",
        requiredMode = RequiredMode.REQUIRED,
        example = "4/0AbCdef..."
    )
    private String code;
}
