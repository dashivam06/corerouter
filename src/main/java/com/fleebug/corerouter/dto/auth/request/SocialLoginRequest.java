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
    title = "Social Login Request",
    description = "Request payload containing provider-issued access token for social sign-in.",
    example = "{\"accessToken\": \"ya29.a0AfH6SM...\"}"
)
public class SocialLoginRequest {

    @NotBlank(message = "Access token is required")
    @Schema(
        description = "OAuth access token issued by Google or GitHub",
        requiredMode = RequiredMode.REQUIRED,
        example = "ya29.a0AfH6SM..."
    )
    private String accessToken;
}
