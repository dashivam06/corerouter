package com.fleebug.corerouter.dto.task.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "Task Insights Response", description = "High-level task execution insights for dashboard cards")
public class TaskInsightsResponse {

    @Schema(description = "Total number of tasks", example = "120")
    private Long totalTasks;

    @Schema(description = "Number of completed tasks", example = "95")
    private Long completed;

    @Schema(description = "Number of failed tasks", example = "8")
    private Long failed;

    @Schema(description = "Number of processing tasks", example = "10")
    private Long processing;

    @Schema(description = "Number of queued tasks", example = "7")
    private Long queued;
}