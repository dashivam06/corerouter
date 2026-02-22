package com.fleebug.corerouter.dto.documentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
    title = "Update API Documentation Request",
    description = "Request to update existing API documentation. Both fields are optional; only provided fields will be updated.",
    example = "{\"title\": \"Chat Completions (Updated)\", \"content\": \"Updated documentation with new parameters...\"}"
)
public class UpdateApiDocumentationRequest {

    @Size(min = 5, max = 255, message = "Title must be between 5 and 255 characters")
    @Schema(
        description = "Updated documentation title. Leave null to keep the existing title.",
        example = "Chat Completions (Updated)",
        minLength = 5,
        maxLength = 255
    )
    private String title;

    @Size(min = 10, message = "Content must be at least 10 characters")
    @Schema(
        description = "Updated documentation content with markdown support. Leave null to keep the existing content.",
        example = "Updated documentation with new features and parameters",
        minLength = 10,
        maxLength = 50000
    )
    private String content;
}
