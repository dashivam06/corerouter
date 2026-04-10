package com.fleebug.corerouter.controller.billing;

import com.fleebug.corerouter.dto.billing.response.UserDashboardInsightsResponse;
import com.fleebug.corerouter.dto.billing.response.UserSpendingResponse;
import com.fleebug.corerouter.dto.billing.response.UserUsageByModelTypeResponse;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.security.details.CustomUserDetails;
import com.fleebug.corerouter.service.billing.UsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "User Dashboard", description = "User dashboard endpoints with separated concerns")
public class UserDashboardController {

    private final UsageService usageService;

    @Operation(summary = "Get dashboard insights", description = "Returns top card insights: balance, active API keys, tasks this month, and today's consumption")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User dashboard insights retrieved successfully")
    })
    @GetMapping("/insights")
    public ResponseEntity<ApiResponse<UserDashboardInsightsResponse>> getDashboardInsights(
            Authentication authentication,
            HttpServletRequest request) {
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();

        UserDashboardInsightsResponse response = usageService.getUserDashboardInsights(
                user.getUserId(),
                user.getBalance()
        );

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "User dashboard insights retrieved successfully", response, request));
    }

    @Operation(summary = "Get dashboard spending", description = "Returns spending summary and daily trend for requested time range")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User dashboard spending retrieved successfully")
    })
    @GetMapping("/spending")
    public ResponseEntity<ApiResponse<UserSpendingResponse>> getDashboardSpending(
            @Parameter(description = "Date filter: today, week, month, 3m, 6m, year, all", example = "month")
            @RequestParam(defaultValue = "month") String dateFilter,
            @Parameter(description = "Custom start date (ISO 8601, optional)", example = "2026-04-01T00:00:00")
            @RequestParam(required = false) LocalDateTime fromDate,
            @Parameter(description = "Custom end date (ISO 8601, optional)", example = "2026-04-11T23:59:59")
            @RequestParam(required = false) LocalDateTime toDate,
            Authentication authentication,
            HttpServletRequest request) {
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();

        DateRange range = resolveDateRange(dateFilter, fromDate, toDate);
        UserSpendingResponse response = usageService.getUserSpending(
                user.getUserId(),
                range.from(),
                range.to(),
                range.appliedFilter()
        );

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "User dashboard spending retrieved successfully", response, request));
    }

    @Operation(summary = "Get usage count by model type", description = "Returns usage request counts grouped by model type for requested time range")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User usage by model type retrieved successfully")
    })
    @GetMapping("/usage-by-model-type")
    public ResponseEntity<ApiResponse<UserUsageByModelTypeResponse>> getUsageByModelType(
            Authentication authentication,
            HttpServletRequest request) {
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();

        UserUsageByModelTypeResponse response = usageService.getUserUsageByModelTypeLifetime(user.getUserId());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "User usage by model type retrieved successfully", response, request));
    }

    private DateRange resolveDateRange(String dateFilter, LocalDateTime fromDate, LocalDateTime toDate) {
        if (fromDate != null && toDate != null) {
            return new DateRange(fromDate, toDate, "custom");
        }

        LocalDateTime now = LocalDateTime.now();
        String normalized = dateFilter == null ? "month" : dateFilter.trim().toLowerCase();
        LocalDateTime from;

        switch (normalized) {
            case "today" -> from = now.toLocalDate().atStartOfDay();
            case "week" -> from = now.minusWeeks(1);
            case "3m", "3month", "3months" -> {
                from = now.minusMonths(3);
                normalized = "3m";
            }
            case "6m", "6month", "6months" -> {
                from = now.minusMonths(6);
                normalized = "6m";
            }
            case "year", "1y", "12m" -> {
                from = now.minusYears(1);
                normalized = "year";
            }
            case "all" -> from = now.minusYears(5);
            default -> {
                from = now.minusMonths(1);
                normalized = "month";
            }
        }

        return new DateRange(from, now, normalized);
    }

    private record DateRange(LocalDateTime from, LocalDateTime to, String appliedFilter) {
    }
}