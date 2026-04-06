package com.fleebug.corerouter.controller.user;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.user.response.UserActivityResponse;
import com.fleebug.corerouter.entity.activity.ActivityLog;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.security.details.CustomUserDetails;
import com.fleebug.corerouter.service.activity.ActivityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/activity")
@RequiredArgsConstructor
@Tag(name = "User Activity", description = "Authenticated user activity logs")
public class UserActivityController {

    private final ActivityLogService activityLogService;

    @Operation(summary = "Get my activity", description = "Returns paginated activity logs for the authenticated user only")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Activity retrieved successfully")
    })
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<Page<UserActivityResponse>>> getMyRecentActivity(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication,
            HttpServletRequest request) {
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();

        Page<ActivityLog> logs = activityLogService.getActivityByUser(user, page, size);
        Page<UserActivityResponse> response = logs.map(log -> UserActivityResponse.builder()
                .action(log.getAction())
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Activity retrieved successfully", response, request));
    }
}
