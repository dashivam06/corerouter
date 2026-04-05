package com.fleebug.corerouter.controller.task;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.task.response.AdminTaskAnalyticsResponse;
import com.fleebug.corerouter.dto.task.response.TaskInsightsResponse;
import com.fleebug.corerouter.enums.task.TaskStatus;
import com.fleebug.corerouter.service.task.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/admin/tasks")
@RequiredArgsConstructor
@Tag(name = "Admin Tasks", description = "Admin task insights")
public class AdminTaskInsightsController {

    private final TaskService taskService;

    @Operation(summary = "Get admin task insights", description = "Get total tasks and task counts by status across all users")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Admin task insights retrieved successfully")
    })
    @GetMapping("/insights")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TaskInsightsResponse>> getTaskInsights(HttpServletRequest request) {
        TaskInsightsResponse response = taskService.getTaskInsightsForAdmin();

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Admin task insights retrieved successfully", response, request));
    }

    @Operation(summary = "Get admin task analytics by date range", description = "Get date-wise task counts with optional status filter (ALL, QUEUED, PROCESSING, COMPLETED, FAILED)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Admin task analytics retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range or status")
    })
    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminTaskAnalyticsResponse>> getTaskAnalytics(
            @Parameter(description = "Start date-time (ISO 8601)", example = "2026-04-01T00:00:00")
            @RequestParam LocalDateTime from,
            @Parameter(description = "End date-time (ISO 8601)", example = "2026-04-05T23:59:59")
            @RequestParam LocalDateTime to,
            @Parameter(description = "Optional status filter", example = "ALL")
            @RequestParam(defaultValue = "ALL") String status,
            HttpServletRequest request) {

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        TaskStatus statusFilter = parseStatusFilter(status);
        AdminTaskAnalyticsResponse response = taskService.getTaskAnalyticsForAdmin(from, to, statusFilter);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Admin task analytics retrieved successfully", response, request));
    }

    private TaskStatus parseStatusFilter(String status) {
        String normalized = status == null ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(normalized)) {
            return null;
        }

        try {
            return TaskStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid status filter. Allowed values: ALL, QUEUED, PROCESSING, COMPLETED, FAILED");
        }
    }
}
