package com.fleebug.corerouter.dto.task.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fleebug.corerouter.enums.task.TaskStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
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
    title = "Async Task Response",
    description = "Response returned when a task is successfully created and queued for processing",
    example = "{\"task_id\": \"f47ac10b-58cc-4372-a567-0e02b2c3d479\", \"status\": \"QUEUED\"}"
)
public class TaskAsyncResponse {

    @Schema(
        description = "Unique task identifier (UUID) for tracking the async task",
        requiredMode = RequiredMode.REQUIRED,
        example = "f47ac10b-58cc-4372-a567-0e02b2c3d479",
        pattern = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
    )
    @JsonProperty("task_id")
    private String taskId;

    @Schema(
        description = "Current status of the task",
        requiredMode = RequiredMode.REQUIRED,
        example = "QUEUED",
        allowableValues = {"QUEUED", "PROCESSING", "COMPLETED", "FAILED"}
    )
    private TaskStatus status;
}

