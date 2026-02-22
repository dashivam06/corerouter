package com.fleebug.corerouter.dto.documentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "API Documentation Response",
    description = "Contains detailed API documentation for a specific model endpoint. Includes title, content, and metadata.",
    example = "{\"docId\": 42, \"title\": \"Chat Completions Endpoint\", \"content\": \"Handles real-time conversations with the model...\", \"modelId\": 1, \"modelName\": \"GPT-4\", \"createdAt\": \"2024-01-15T10:30:00Z\", \"updatedAt\": \"2024-02-22T14:20:00Z\"}"
)
public class ApiDocumentationResponse {

    @Schema(
        description = "Unique identifier for this documentation resource",
        requiredMode = RequiredMode.REQUIRED,
        example = "42"
    )
    private Integer docId;

    @Schema(
        description = "Title of the documentation section",
        requiredMode = RequiredMode.REQUIRED,
        example = "Chat Completions Endpoint",
        maxLength = 255
    )
    private String title;

    @Schema(
        description = "Full documentation content with markdown formatting support. Includes code examples, parameters, request/response formats.",
        requiredMode = RequiredMode.REQUIRED,
        example = "This endpoint handles real-time conversations with the model. Send a messages array containing conversation history...",
        maxLength = 50000
    )
    private String content;

    @Schema(
        description = "Foreign key reference to the model this documentation belongs to",
        requiredMode = RequiredMode.REQUIRED,
        example = "1"
    )
    private Integer modelId;

    @Schema(
        description = "Display name of the associated model",
        requiredMode = RequiredMode.REQUIRED,
        example = "GPT-4",
        maxLength = 255
    )
    private String modelName;

    @Schema(
        description = "Timestamp when this documentation was created",
        requiredMode = RequiredMode.REQUIRED,
        example = "2024-01-15T10:30:00Z",
        format = "date-time"
    )
    private LocalDateTime createdAt;

    @Schema(
        description = "Timestamp of the last documentation update",
        requiredMode = RequiredMode.REQUIRED,
        example = "2024-02-22T14:20:00Z",
        format = "date-time"
    )
    private LocalDateTime updatedAt;
}
