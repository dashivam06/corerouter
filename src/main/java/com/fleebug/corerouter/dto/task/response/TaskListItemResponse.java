package com.fleebug.corerouter.dto.task.response;

import com.fleebug.corerouter.enums.task.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "Task List Item", description = "A single task item in admin paginated task list")
public class TaskListItemResponse {

    @Schema(description = "Task ID", example = "b3b98652-5f58-4b4f-a573-6f4ac9b24d92")
    private String taskId;

    @Schema(description = "Task status", example = "QUEUED")
    private TaskStatus status;

    @Schema(description = "API key ID associated with task", example = "14")
    private Integer apiKeyId;

    @Schema(description = "Model ID associated with task", example = "3")
    private Integer modelId;

    @Schema(description = "Task created timestamp", example = "2026-04-05T09:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Task updated timestamp", example = "2026-04-05T09:01:10")
    private LocalDateTime updatedAt;

    @Schema(description = "Task completed timestamp", example = "2026-04-05T09:01:10")
    private LocalDateTime completedAt;

    @Schema(description = "Task processing time in milliseconds", example = "721")
    private Long processingTimeMs;
}