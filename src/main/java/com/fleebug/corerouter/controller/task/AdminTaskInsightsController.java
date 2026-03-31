package com.fleebug.corerouter.controller.task;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.task.response.TaskInsightsResponse;
import com.fleebug.corerouter.service.task.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
