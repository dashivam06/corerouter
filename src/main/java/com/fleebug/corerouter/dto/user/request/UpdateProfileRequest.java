package com.fleebug.corerouter.dto.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "Update Profile Request",
    description = "Request payload to update user profile details."
)
public class UpdateProfileRequest {

    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Schema(description = "New full name", example = "John Doe")
    private String fullName;

    @Size(max = 255, message = "Profile image URL must not exceed 255 characters")
    @Schema(description = "Profile image URL", example = "https://cdn.example.com/profiles/user-123.png")
    private String profileImage;

    @NotNull(message = "Email subscription preference is required")
    @Schema(description = "Whether the user wants to receive email updates", example = "true")
    private Boolean emailSubscribed;
}
