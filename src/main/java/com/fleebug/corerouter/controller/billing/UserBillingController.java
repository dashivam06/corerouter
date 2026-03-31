package com.fleebug.corerouter.controller.billing;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.fleebug.corerouter.dto.billing.response.UserBillingInsightsResponse;
import com.fleebug.corerouter.dto.billing.response.UsageRecordResponse;
import com.fleebug.corerouter.dto.billing.response.UsageSummaryResponse;
import com.fleebug.corerouter.dto.billing.response.UserUsageInsightsResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@Tag(name = "User Billing", description = "Usage and cost queries for authenticated users")
public class UserBillingController {

    private final UsageService usageService;
    private final TelemetryClient telemetryClient;

    /**
     * Get usage records for a specific task.
     *
     * @param taskId  task ID
     * @param request HTTP servlet request
     * @return list of usage records
     */
    @Operation(summary = "Get usage by task", description = "Retrieve all usage records for a specific task")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usage records retrieved successfully")
    })
    @GetMapping("/usage/task/{taskId}")
    public ResponseEntity<ApiResponse<List<UsageRecordResponse>>> getUsageByTask(
            @Parameter(description = "Task ID", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479") @PathVariable String taskId,
            HttpServletRequest request) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("taskId", taskId);
        telemetryClient.trackTrace("User: get usage for task", SeverityLevel.Information, properties);

        List<UsageRecordResponse> records = usageService.getUsageByTaskId(taskId);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Usage records retrieved successfully", records, request));
    }

    /**
     * Get usage summary for an API key grouped by unit type.
     *
     * @param apiKeyId API key ID
     * @param from     period start
     * @param to       period end
     * @param request  HTTP servlet request
     * @return usage summary with breakdown
     */
    @Operation(summary = "Usage summary by unit type", description = "Get aggregated usage summary for an API key grouped by unit type within a date range")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usage summary retrieved successfully")
    })
    @GetMapping("/usage/apikey/{apiKeyId}/summary")
    public ResponseEntity<ApiResponse<UsageSummaryResponse>> getUsageSummary(
            @Parameter(description = "API key ID", example = "10") @PathVariable Integer apiKeyId,
            @Parameter(description = "Period start (ISO 8601)", example = "2026-03-01T00:00:00") @RequestParam LocalDateTime from,
            @Parameter(description = "Period end (ISO 8601)", example = "2026-03-31T23:59:59") @RequestParam LocalDateTime to,
            HttpServletRequest request) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("apiKeyId", String.valueOf(apiKeyId));
        telemetryClient.trackTrace("User: get usage summary for apiKey", SeverityLevel.Information, properties);

        UsageSummaryResponse response = usageService.getUsageSummaryByApiKey(apiKeyId, from, to);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Usage summary retrieved successfully", response, request));
    }

    /**
     * Get usage summary for an API key grouped by model and unit type.
     *
     * @param apiKeyId API key ID
     * @param from     period start
     * @param to       period end
     * @param request  HTTP servlet request
     * @return usage summary with per-model breakdown
     */
    @Operation(summary = "Usage summary by model", description = "Get aggregated usage summary for an API key grouped by model and unit type within a date range")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usage summary retrieved successfully")
    })
    @GetMapping("/usage/apikey/{apiKeyId}/summary/by-model")
    public ResponseEntity<ApiResponse<UsageSummaryResponse>> getUsageSummaryByModel(
            @Parameter(description = "API key ID", example = "10") @PathVariable Integer apiKeyId,
            @Parameter(description = "Period start (ISO 8601)", example = "2026-03-01T00:00:00") @RequestParam LocalDateTime from,
            @Parameter(description = "Period end (ISO 8601)", example = "2026-03-31T23:59:59") @RequestParam LocalDateTime to,
            HttpServletRequest request) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("apiKeyId", String.valueOf(apiKeyId));
        telemetryClient.trackTrace("User: get usage summary by model for apiKey", SeverityLevel.Information, properties);

        UsageSummaryResponse response = usageService.getUsageSummaryByApiKeyGroupedByModel(apiKeyId, from, to);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Usage summary by model retrieved successfully", response, request));
    }

    /**
     * Get paginated usage history for an API key.
     *
     * @param apiKeyId API key ID
     * @param from     period start
     * @param to       period end
     * @param page     page number (default 0)
     * @param size     page size (default 20)
     * @param request  HTTP servlet request
     * @return page of usage records
     */
    @Operation(summary = "Usage history (paginated)", description = "Get paginated usage history for an API key sorted by most recent first")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usage history retrieved successfully")
    })
    @GetMapping("/usage/apikey/{apiKeyId}/history")
    public ResponseEntity<ApiResponse<Page<UsageRecordResponse>>> getUsageHistory(
            @Parameter(description = "API key ID", example = "10") @PathVariable Integer apiKeyId,
            @Parameter(description = "Period start (ISO 8601)", example = "2026-03-01T00:00:00") @RequestParam LocalDateTime from,
            @Parameter(description = "Period end (ISO 8601)", example = "2026-03-31T23:59:59") @RequestParam LocalDateTime to,
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("apiKeyId", String.valueOf(apiKeyId));
        properties.put("page", String.valueOf(page));
        properties.put("size", String.valueOf(size));
        telemetryClient.trackTrace("User: get usage history", SeverityLevel.Information, properties);

        Page<UsageRecordResponse> records = usageService.getUsageHistory(
                apiKeyId, from, to, PageRequest.of(page, size, Sort.by("recordedAt").descending()));

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Usage history retrieved successfully", records, request));
    }

    /**
     * Get total cost for the authenticated user across all API keys.
     *
     * @param from           period start
     * @param to             period end
     * @param authentication current user authentication
     * @param request        HTTP servlet request
     * @return total cost
     */
    @Operation(summary = "Get total cost", description = "Get total cost for the authenticated user across all their API keys within a date range")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Total cost retrieved successfully")
    })
    @GetMapping("/cost/total")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalCost(
            @Parameter(description = "Period start (ISO 8601)", example = "2026-03-01T00:00:00") @RequestParam LocalDateTime from,
            @Parameter(description = "Period end (ISO 8601)", example = "2026-03-31T23:59:59") @RequestParam LocalDateTime to,
            Authentication authentication,
            HttpServletRequest request) {
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        
        Map<String, String> properties = new HashMap<>();
        properties.put("userId", String.valueOf(user.getUserId()));
        telemetryClient.trackTrace("User: get total cost", SeverityLevel.Information, properties);

        BigDecimal totalCost = usageService.getTotalCostByUser(user.getUserId(), from, to);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Total cost retrieved successfully", totalCost, request));
    }

    @Operation(summary = "Get billing insights", description = "Get current balance, credits used this month, and change from last month for the authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Billing insights retrieved successfully")
    })
    @GetMapping("/insights")
    public ResponseEntity<ApiResponse<UserBillingInsightsResponse>> getBillingInsights(
            Authentication authentication,
            HttpServletRequest request) {
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thisMonthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime lastMonthStart = thisMonthStart.minusMonths(1);
        LocalDateTime comparableLastMonthEnd = lastMonthStart.plusSeconds(Duration.between(thisMonthStart, now).getSeconds());

        BigDecimal creditsUsedThisMonth = usageService.getTotalCostByUser(user.getUserId(), thisMonthStart, now);
        BigDecimal creditsUsedComparableLastMonth = usageService.getTotalCostByUser(user.getUserId(), lastMonthStart, comparableLastMonthEnd);

        UserBillingInsightsResponse insights = UserBillingInsightsResponse.builder()
                .currentBalance(user.getBalance().setScale(2, RoundingMode.HALF_UP))
                .creditsUsedThisMonth(creditsUsedThisMonth.setScale(2, RoundingMode.HALF_UP))
                .creditsUsedChangeFromLastMonthPercent(
                        calculatePercentChange(creditsUsedThisMonth, creditsUsedComparableLastMonth)
                )
                .build();

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Billing insights retrieved successfully", insights, request));
    }

    @Operation(summary = "Get usage insights", description = "Get dashboard insights for authenticated user usage page")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usage insights retrieved successfully")
    })
    @GetMapping("/usage/insights")
    public ResponseEntity<ApiResponse<UserUsageInsightsResponse>> getUsageInsights(
            Authentication authentication,
            HttpServletRequest request) {
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();

        UserUsageInsightsResponse insights = usageService.getUserUsageInsights(user.getUserId());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Usage insights retrieved successfully", insights, request));
    }

    private BigDecimal calculatePercentChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }

}
