package com.fleebug.corerouter.dto.documentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "Create API Documentation Request",
    description = "Request to create new API documentation for a model. Includes title and detailed content describing API usage, parameters, and examples.",
    example = "{\"title\": \"Chat Completions Endpoint\", \"content\": \"This endpoint handles chat-based conversations with the model. Accepts messages array and returns AI-generated response...\"}"
)
public class CreateApiDocumentationRequest {

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 5, max = 255, message = "Title must be between 5 and 255 characters")
    @Schema(
        description = "Title/heading for the documentation section (e.g., 'Chat Completions', 'Image Generation', 'Authentication')",
        requiredMode = RequiredMode.REQUIRED,
        example = "Chat Completions Endpoint",
        minLength = 5,
        maxLength = 255
    )
    private String title;

    @NotBlank(message = "Content cannot be blank")
    @Size(min = 10, message = "Content must be at least 10 characters")
    @Schema(
        description = "Detailed documentation content. Can include markdown formatting, code examples, parameters, response formats, and usage instructions.",
        requiredMode = RequiredMode.REQUIRED,
        example = "This endpoint handles chat-based conversations. Accepts a messages array containing conversation history. Returns streaming or complete responses with usage information.",
        minLength = 10,
        maxLength = 50000
    )
    private String content;
}
