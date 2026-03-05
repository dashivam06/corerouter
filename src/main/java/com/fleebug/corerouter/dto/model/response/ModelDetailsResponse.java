package com.fleebug.corerouter.dto.model.response;

import com.fleebug.corerouter.dto.documentation.response.ApiDocumentationResponse;
import com.fleebug.corerouter.enums.model.ModelStatus;
import com.fleebug.corerouter.enums.model.ModelType;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "Model Details Response",
    description = "Comprehensive model information including all configuration details and associated documentation. Extends basic model response with related documentation."
)
public class ModelDetailsResponse {

    @Schema(
        description = "Unique database identifier for the model",
        requiredMode = RequiredMode.REQUIRED,
        example = "1"
    )
    private Integer modelId;

    @Schema(
        description = "Display name of the model",
        requiredMode = RequiredMode.REQUIRED,
        example = "MISTRAL-7B",
        maxLength = 255
    )
    private String fullname;

    @Schema(
        description = "Unique identifier/slug for the model",
        requiredMode = RequiredMode.REQUIRED,
        example = "mistral7b"
    )
    private String username;

    @Schema(
        description = "AI service provider",
        requiredMode = RequiredMode.REQUIRED,
        example = "MISTRAL"
    )
    private String provider;

    @Schema(description = "Current status", example = "ACTIVE")
    private ModelStatus status;

    @Schema(
        description = "API endpoint URL for requests",
        requiredMode = RequiredMode.REQUIRED,
        example = "https://api.mistral.com/v1/chat/completions"
    )
    private String endpointUrl;

    @Schema(
        description = "Type/classification of the model",
        requiredMode = RequiredMode.REQUIRED,
        example = "LLM",
        enumAsRef = true
    )
    private ModelType type;

    @Schema(
        description = "Model description and capabilities",
        example = "Most capable model available"
    )
    private String description;

    @Schema(
        description = "Timestamp when the model was created",
        requiredMode = RequiredMode.REQUIRED,
        example = "2024-01-15T10:30:00Z",
        format = "date-time"
    )
    private LocalDateTime createdAt;

    @Schema(
        description = "Timestamp of the last update",
        requiredMode = RequiredMode.REQUIRED,
        example = "2024-02-22T14:20:00Z",
        format = "date-time"
    )
    private LocalDateTime updatedAt;

    @Schema(
        description = "List of API documentation resources associated with this model. Includes usage examples, endpoints, and technical details.",
        requiredMode = RequiredMode.REQUIRED
    )
    private List<ApiDocumentationResponse> documentation;
}
