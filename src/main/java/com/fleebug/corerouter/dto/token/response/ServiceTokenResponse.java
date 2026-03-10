package com.fleebug.corerouter.dto.token.response;

import java.time.LocalDateTime;

import com.fleebug.corerouter.enums.token.ServiceRole;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "Service Token Response", description = "Service token metadata response (never contains token hash)")
public class ServiceTokenResponse {

    @Schema(description = "Database primary key", example = "1")
    private Long id;

    @Schema(description = "Public token identifier used for lookup", example = "a1b2c3d4e5f6")
    private String tokenId;

    @Schema(description = "Unique display name of the token", example = "ocr-worker-1")
    private String name;

    @Schema(description = "Role assigned to this token", example = "WORKER")
    private ServiceRole role;

    @Schema(description = "Whether the token is active", example = "true")
    private boolean active;

    @Schema(description = "Creation timestamp", example = "2026-03-11T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last successful usage timestamp", example = "2026-03-11T12:45:00")
    private LocalDateTime lastUsedAt;
}
