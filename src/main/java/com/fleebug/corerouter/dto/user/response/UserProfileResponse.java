package com.fleebug.corerouter.dto.user.response;

import com.fleebug.corerouter.enums.user.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "User Profile Response",
    description = "Profile details for the authenticated user."
)
public class UserProfileResponse {

    @Schema(description = "User ID", example = "101")
    private Integer userId;

    @Schema(description = "Full name", example = "John Doe")
    private String fullName;

    @Schema(description = "Email address", example = "john@example.com")
    private String email;

    @Schema(description = "Profile image URL", example = "https://cdn.example.com/profiles/user-123.png")
    private String profileImage;

    @Schema(description = "Email subscription preference", example = "true")
    private Boolean emailSubscribed;

    @Schema(description = "User status", example = "ACTIVE")
    private UserStatus status;
}
