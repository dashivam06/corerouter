package com.fleebug.corerouter.dto.task.request;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "Task Creation Request",
    description = "Request payload for creating a new async task with model inference parameters",
    example = "{\"apiKeyId\": 123, \"modelId\": 456, \"payload\": {\"prompt\": \"Hello world\", \"temperature\": 0.7}}"
)
public class TaskCreateRequest {

    @Schema(
        description = "API key identifier for authentication and billing tracking",
        requiredMode = RequiredMode.REQUIRED,
        example = "123",
        minimum = "1"
    )
    @NotNull(message = "apiKeyId is required")
    private Integer apiKeyId;

    @Schema(
        description = "Model identifier specifying which AI model to use for the task",
        requiredMode = RequiredMode.REQUIRED,
        example = "456",
        minimum = "1"
    )
    @NotNull(message = "modelId is required")
    private Integer modelId;

    @Schema(
        description = "Arbitrary inference payload containing model-specific parameters (prompt, temperature, input data, etc.)",
        requiredMode = RequiredMode.REQUIRED,
        example = "{\"prompt\": \"Explain quantum physics\", \"temperature\": 0.7, \"max_tokens\": 100}"
    )
    @NotNull(message = "payload is required")
    private Map<String, Object> payload;
}

