package com.fleebug.corerouter.controller.user;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.user.response.AdminUserInsightsResponse;
import com.fleebug.corerouter.dto.user.response.UserAnalyticsResponse;
import com.fleebug.corerouter.dto.user.response.PaginatedUserListResponse;
import com.fleebug.corerouter.enums.user.UserRole;
import com.fleebug.corerouter.enums.user.UserStatus;
import com.fleebug.corerouter.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "Admin user insights")
public class AdminUserInsightsController {

    private final UserService userService;

    @Operation(summary = "Get user insights", description = "Get total users, growth, active/inactive split, suspended users, and admin users for dashboard")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User insights retrieved successfully")
    })
    @GetMapping("/insights")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserInsightsResponse>> getUserInsights(HttpServletRequest request) {
        AdminUserInsightsResponse insights = userService.getAdminInsights();

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "User insights retrieved successfully", insights, request));
    }

    @Operation(summary = "Get user analytics by date range", description = "Get daily user creation, deletion, and revocation counts for a specific date range. Returns counts per day and totals.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User analytics retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range")
    })
    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserAnalyticsResponse>> getUserAnalytics(
            @Parameter(description = "Start date (ISO 8601)", example = "2026-03-26T00:00:00")
            @RequestParam java.time.LocalDateTime from,
            @Parameter(description = "End date (ISO 8601)", example = "2026-04-04T23:59:59")
            @RequestParam java.time.LocalDateTime to,
            HttpServletRequest request) {

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        UserAnalyticsResponse analytics = userService.getUserAnalyticsByDateRange(from, to);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "User analytics retrieved successfully", analytics, request));
    }

    @Operation(summary = "Get paginated user list", description = "Get paginated list of users with optional filtering by role and status")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User list retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    })
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaginatedUserListResponse>> getUsersList(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by user role (USER, ADMIN, or null for all)", example = "USER")
            @RequestParam(required = false) UserRole role,
            @Parameter(description = "Filter by user status (ACTIVE, INACTIVE, BANNED, SUSPENDED, DELETED, or null for all)", example = "ACTIVE")
            @RequestParam(required = false) UserStatus status,
            HttpServletRequest request) {

        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Page must be >= 0 and size must be > 0");
        }

        PaginatedUserListResponse users = userService.getUsersWithFilters(page, size, role, status);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Users retrieved successfully", users, request));
    }
}
