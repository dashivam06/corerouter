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
    title = "Authenticated User Info",
    description = "Basic user profile returned alongside authentication tokens."
)
public class AuthUserInfoResponse {

    @Schema(
        description = "User full name",
        requiredMode = RequiredMode.REQUIRED,
        example = "John Doe"
    )
    private String fullName;

    @Schema(
        description = "User email address",
        requiredMode = RequiredMode.REQUIRED,
        example = "john@example.com"
    )
    private String email;

    @Schema(
        description = "User profile image URL",
        requiredMode = RequiredMode.NOT_REQUIRED,
        example = "https://example.com/profile.jpg"
    )
    private String profileImage;
}
