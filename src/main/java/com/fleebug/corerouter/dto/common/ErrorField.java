package com.fleebug.corerouter.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a validation error for a single field
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    title = "Error Field",
    description = "Represents a validation error for a specific field with field name and error message",
    example = "{\"field\": \"email\", \"message\": \"Email must be valid\"}"
)
public class ErrorField {
    
    @Schema(
        description = "Name of the field that failed validation",
        requiredMode = RequiredMode.REQUIRED,
        example = "email"
    )
    private String field;
    
    @Schema(
        description = "Error message describing the validation failure",
        requiredMode = RequiredMode.REQUIRED,
        example = "Email must be valid"
    )
    private String message;
}
