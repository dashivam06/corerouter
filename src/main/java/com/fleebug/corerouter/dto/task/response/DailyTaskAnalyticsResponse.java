package com.fleebug.corerouter.dto.task.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "Daily Task Analytics", description = "Task counts grouped by status for a single day")
public class DailyTaskAnalyticsResponse {

    @Schema(description = "Date bucket", example = "2026-04-05")
    private LocalDate date;

    @Schema(description = "Total tasks for this date bucket", example = "30")
    private long total;

    @Schema(description = "Queued tasks for this date bucket", example = "7")
    private long queued;

    @Schema(description = "Processing tasks for this date bucket", example = "10")
    private long processing;

    @Schema(description = "Completed tasks for this date bucket", example = "11")
    private long completed;

    @Schema(description = "Failed tasks for this date bucket", example = "2")
    private long failed;
}