package com.fleebug.corerouter.dto.task.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "Admin Task Analytics Response", description = "Date-wise task analytics with optional status filter")
public class AdminTaskAnalyticsResponse {

    @Schema(description = "Start date used for analytics", example = "2026-04-01")
    private LocalDate fromDate;

    @Schema(description = "End date used for analytics", example = "2026-04-05")
    private LocalDate toDate;

    @Schema(description = "Applied status filter (ALL, QUEUED, PROCESSING, COMPLETED, FAILED)", example = "ALL")
    private String statusFilter;

    @Schema(description = "Daily task analytics sorted by date ascending")
    private List<DailyTaskAnalyticsResponse> dailyAnalytics;

    @Schema(description = "Total tasks in range after status filter", example = "120")
    private long totalTasks;

    @Schema(description = "Completed tasks in range after status filter", example = "80")
    private long completed;

    @Schema(description = "Failed tasks in range after status filter", example = "9")
    private long failed;

    @Schema(description = "Processing tasks in range after status filter", example = "16")
    private long processing;

    @Schema(description = "Queued tasks in range after status filter", example = "15")
    private long queued;
}