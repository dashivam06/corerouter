package com.fleebug.corerouter.controller.user;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.user.response.AdminUserInsightsResponse;
import com.fleebug.corerouter.service.user.UserService;
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
}
