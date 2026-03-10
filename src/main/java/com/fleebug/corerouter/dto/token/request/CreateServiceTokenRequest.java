package com.fleebug.corerouter.dto.token.request;

import com.fleebug.corerouter.enums.token.ServiceRole;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "Create Service Token Request", description = "Payload to create a new service token")
public class CreateServiceTokenRequest {

    @NotBlank(message = "Token name is required")
    @Schema(description = "Unique human-readable name for the token", example = "ocr-worker-1")
    private String name;

    @NotNull(message = "Role is required")
    @Schema(description = "Role assigned to this service token", example = "WORKER")
    private ServiceRole role;
}
