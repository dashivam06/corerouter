package com.fleebug.corerouter.dto.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
    title = "Delete Account Request",
    description = "Request payload for soft deleting user account."
)
public class DeleteAccountRequest {

    @NotBlank(message = "Password is required")
    @Schema(description = "Current account password used to confirm account deletion", example = "MyCurrentPass@123")
    private String password;
}
