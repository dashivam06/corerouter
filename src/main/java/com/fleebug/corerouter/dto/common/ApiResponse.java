package com.fleebug.corerouter.dto.common;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    title = "API Response",
    description = "Standard API response wrapper for all endpoints. Contains status information, message, and generic data payload.",
    example = "{\"timestamp\": \"2024-02-22T10:30:45Z\", \"status\": 200, \"success\": true, \"message\": \"Request successful\", \"path\": \"/api/v1/users\", \"method\": \"GET\", \"data\": {}}"
)
public class ApiResponse<T> {

    @Schema(
        description = "Server timestamp when the request was processed (ISO 8601 format)",
        requiredMode = RequiredMode.REQUIRED,
        example = "2024-02-22T10:30:45Z",
        format = "date-time"
    )
    private LocalDateTime timestamp;

    @Schema(
        description = "HTTP status code of the response (200, 201, 400, 401, 403, 404, 409, 500, etc.)",
        requiredMode = RequiredMode.REQUIRED,
        example = "200",
        minimum = "100",
        maximum = "599"
    )
    private int status;

    @Schema(
        description = "Boolean indicator of request success. True for 2xx status codes, false for 4xx/5xx.",
        requiredMode = RequiredMode.REQUIRED,
        example = "true"
    )
    private boolean success;

    @Schema(
        description = "Human-readable message describing the response result or error. Provides context for the status code.",
        requiredMode = RequiredMode.REQUIRED,
        example = "Request successful",
        maxLength = 1000
    )
    private String message;

    @Schema(
        description = "The request path/endpoint that was called (including query parameters if any)",
        requiredMode = RequiredMode.REQUIRED,
        example = "/api/v1/users"
    )
    private String path;

    @Schema(
        description = "HTTP method used for the request (GET, POST, PUT, DELETE, PATCH, etc.)",
        requiredMode = RequiredMode.REQUIRED,
        example = "GET",
        allowableValues = {"GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"}
    )
    private String method;

    @Schema(
        description = "Generic data payload containing the actual response content. Type depends on the specific endpoint. Null if no data is returned.",
        example = "{}"
    )
    private T data;
}
