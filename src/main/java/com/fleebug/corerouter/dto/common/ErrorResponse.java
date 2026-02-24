package com.fleebug.corerouter.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Extended API response for errors with field-level validation details
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    title = "Error Response",
    description = "Extended API response wrapper for errors. Contains status, message, and field-specific validation errors when applicable.",
    example = "{\"timestamp\": \"2024-02-22T10:30:45Z\", \"status\": 400, \"success\": false, \"message\": \"Validation failed\", \"path\": \"/api/v1/users\", \"method\": \"POST\", \"errors\": [{\"field\": \"email\", \"message\": \"Email must be valid\"}]}"
)
public class ErrorResponse {
    
    @Schema(
        description = "Server timestamp when the error occurred (ISO 8601 format)",
        requiredMode = RequiredMode.REQUIRED,
        example = "2024-02-22T10:30:45Z",
        format = "date-time"
    )
    private LocalDateTime timestamp;
    
    @Schema(
        description = "HTTP status code (400, 401, 403, 404, 409, 500, etc.)",
        requiredMode = RequiredMode.REQUIRED,
        example = "400",
        minimum = "100",
        maximum = "599"
    )
    private int status;
    
    @Schema(
        description = "Boolean indicator - always false for error responses",
        requiredMode = RequiredMode.REQUIRED,
        example = "false"
    )
    private boolean success;
    
    @Schema(
        description = "Error message describing what went wrong",
        requiredMode = RequiredMode.REQUIRED,
        example = "Validation failed",
        maxLength = 1000
    )
    private String message;
    
    @Schema(
        description = "The request path/endpoint that was called",
        requiredMode = RequiredMode.REQUIRED,
        example = "/api/v1/users"
    )
    private String path;
    
    @Schema(
        description = "HTTP method used (GET, POST, PUT, DELETE, PATCH, etc.)",
        requiredMode = RequiredMode.REQUIRED,
        example = "POST",
        allowableValues = {"GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"}
    )
    private String method;
    
    @Schema(
        description = "List of field-specific validation errors. Present only for validation-related errors.",
        example = "[{\"field\": \"email\", \"message\": \"Email must be valid\"}]"
    )
    private List<ErrorField> errors;
}
