package com.fleebug.corerouter.dto.task.response;

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
    title = "Task Status Response",
    description = "Current status and result information for a specific task",
    example = "{\"status\": \"COMPLETED\", \"result\": {\"output\": \"Generated response\", \"tokens\": 95}}"
)
public class TaskStatusResponse {

    @Schema(
        description = "Current status of the task",
        requiredMode = RequiredMode.REQUIRED,
        example = "COMPLETED",
        allowableValues = {"QUEUED", "PROCESSING", "COMPLETED", "FAILED"}
    )
    private TaskStatus status;

    @Schema(
        description = "Task result payload. Contains model output when completed, error details when failed, or null when still processing.",
        example = "{\"output\": \"The generated text response\", \"usage\": {\"total_tokens\": 95}}"
    )
    private Object result;
}

