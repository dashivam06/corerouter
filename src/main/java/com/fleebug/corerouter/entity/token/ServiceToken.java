package com.fleebug.corerouter.entity.token;

import java.time.LocalDateTime;

import com.fleebug.corerouter.enums.token.ServiceRole;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "service_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(title = "Service Token", description = "Represents a hashed service token used to authenticate workers and internal services")
public class ServiceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Auto-generated primary key", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    @Schema(description = "Public token identifier used for fast DB lookup (prefix of the raw token)", example = "a1b2c3d4e5f6")
    private String tokenId;

    @Column(nullable = false, unique = true)
    @Schema(description = "Unique human-readable name for the token (e.g. 'ocr-worker-1')", example = "ocr-worker-1")
    private String name;

    @Column(nullable = false, length = 255)
    @Schema(description = "BCrypt hash of the raw token — never exposed in responses", hidden = true)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "Role granted to this service token", example = "WORKER")
    private ServiceRole role;

    @Column(nullable = false)
    @Schema(description = "Whether this token is currently active", example = "true")
    private boolean active;

    @Schema(description = "Timestamp when the token was created", example = "2026-03-11T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp of the last successful authentication with this token", example = "2026-03-11T12:45:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime lastUsedAt;

}