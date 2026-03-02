package com.fleebug.corerouter.dto.task.request;

import com.fleebug.corerouter.enums.task.TaskStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "Task Status Update Request",
    description = "Request payload for updating task status and optionally providing results",
    example = "{\"taskId\": \"uuid-1234\", \"status\": \"COMPLETED\", \"result\": {\"output\": \"Generated text\"}}"
)
public class TaskStatusUpdateRequest {

    @Schema(
        description = "Unique task identifier (UUID) for the task to update",
        requiredMode = RequiredMode.REQUIRED,
        example = "f47ac10b-58cc-4372-a567-0e02b2c3d479",
        pattern = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
    )
    @NotBlank(message = "taskId is required")
    private String taskId;

    @Schema(
        description = "New status for the task",
        requiredMode = RequiredMode.REQUIRED,
        example = "COMPLETED",
        allowableValues = {"QUEUED", "PROCESSING", "COMPLETED", "FAILED"}
    )
    @NotNull(message = "status is required")
    private TaskStatus status;

    @Schema(
        description = "Optional result payload when marking task as completed. Contains model output, error details, or processing results.",
        example = "{\"output\": \"Generated response text\", \"tokens_used\": 150}"
    )
    private Object result;
}

