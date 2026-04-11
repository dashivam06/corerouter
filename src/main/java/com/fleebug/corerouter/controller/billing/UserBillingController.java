package com.fleebug.corerouter.controller.billing;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.fleebug.corerouter.dto.billing.response.UserBillingInsightsResponse;
import com.fleebug.corerouter.dto.billing.response.UserBalanceHistoryResponse;
import com.fleebug.corerouter.dto.billing.response.UsageRecordResponse;
import com.fleebug.corerouter.dto.billing.response.UsageSummaryResponse;
import com.fleebug.corerouter.dto.billing.response.UserUsageHistoryResponse;
import com.fleebug.corerouter.dto.billing.response.UserUsageInsightsResponse;
import com.fleebug.corerouter.dto.billing.response.TransactionResponse;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.entity.payment.Transaction;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.payment.TransactionStatus;
import com.fleebug.corerouter.enums.payment.TransactionType;
import com.fleebug.corerouter.security.details.CustomUserDetails;
import com.fleebug.corerouter.service.billing.UsageService;
import com.fleebug.corerouter.service.payment.TransactionService;
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
    private final TransactionService transactionService;
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
    @Operation(summary = "Get total charged amount", description = "Get total charged amount for the authenticated user across all completed tasks within a date range")
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
        telemetryClient.trackTrace("User: get total charged amount", SeverityLevel.Information, properties);

        BigDecimal totalCost = usageService.getTotalCostByUser(user.getUserId(), from, to);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Total charged amount retrieved successfully", totalCost, request));
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

        UserBillingInsightsResponse insights = usageService.getUserBillingInsights(
            user.getUserId(),
            user.getBalance()
        );

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Billing insights retrieved successfully", insights, request));
    }

    @Operation(summary = "Get usage insights", description = "Get dashboard insights for authenticated user usage page")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usage insights retrieved successfully")
    })
    @GetMapping("/usage/insights")
    public ResponseEntity<ApiResponse<UserUsageInsightsResponse>> getUsageInsights(
            @Parameter(description = "Period: 7days, 15days, 30days, 3m, 6m, year", example = "30days")
            @RequestParam(defaultValue = "30days") String period,
            @Parameter(description = "Custom start date (ISO 8601, optional)", example = "2026-04-01T00:00:00")
            @RequestParam(required = false) LocalDateTime fromDate,
            @Parameter(description = "Custom end date (ISO 8601, optional)", example = "2026-04-11T23:59:59")
            @RequestParam(required = false) LocalDateTime toDate,
            Authentication authentication,
            HttpServletRequest request) {
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        DateRange dateRange = resolveBillingDateRange(period, fromDate, toDate);

        UserUsageInsightsResponse insights = usageService.getUserUsageInsights(
                user.getUserId(),
                dateRange.period(),
                dateRange.from(),
                dateRange.to());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Usage insights retrieved successfully", insights, request));
    }

    @Operation(summary = "Get daily usage history", description = "Get per-day usage unit breakdown with pricing for authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Daily usage history retrieved successfully")
    })
    @GetMapping("/usage/history")
    public ResponseEntity<ApiResponse<UserUsageHistoryResponse>> getDailyUsageHistory(
            @Parameter(description = "Period: 7days, 15days, 30days, 3m, 6m, year", example = "30days")
            @RequestParam(defaultValue = "30days") String period,
            @Parameter(description = "Custom start date (ISO 8601, optional)", example = "2026-04-01T00:00:00")
            @RequestParam(required = false) LocalDateTime fromDate,
            @Parameter(description = "Custom end date (ISO 8601, optional)", example = "2026-04-11T23:59:59")
            @RequestParam(required = false) LocalDateTime toDate,
            Authentication authentication,
            HttpServletRequest request) {
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        DateRange dateRange = resolveBillingDateRange(period, fromDate, toDate);

        UserUsageHistoryResponse response = usageService.getUserUsageHistory(
                user.getUserId(),
                dateRange.period(),
                dateRange.from(),
                dateRange.to());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Daily usage history retrieved successfully", response, request));
    }

    @Operation(summary = "Get balance history", description = "Get user balance history (credit/debit timeline) by period")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Balance history retrieved successfully")
    })
    @GetMapping({"/balance-history", "/transactions/balance-history"})
    public ResponseEntity<ApiResponse<UserBalanceHistoryResponse>> getBalanceHistory(
            @Parameter(description = "Period: 7days, 15days, 30days, 3m, 6m, year", example = "30days")
            @RequestParam(defaultValue = "30days") String period,
            @Parameter(description = "Custom start date (ISO 8601, optional)", example = "2026-04-01T00:00:00")
            @RequestParam(required = false) LocalDateTime fromDate,
            @Parameter(description = "Custom end date (ISO 8601, optional)", example = "2026-04-11T23:59:59")
            @RequestParam(required = false) LocalDateTime toDate,
            Authentication authentication,
            HttpServletRequest request) {
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        DateRange dateRange = resolveBillingDateRange(period, fromDate, toDate);

        UserBalanceHistoryResponse response = transactionService.getUserBalanceHistory(
            user.getUserId(),
            user.getBalance(),
            dateRange.from(),
            dateRange.to(),
            dateRange.period()
        );

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Balance history retrieved successfully", response, request));
    }

    @Operation(summary = "Get user transaction history", description = "Get paginated user transaction history with filter by type/status/date/search")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully")
    })
    @GetMapping({"/transaction-history", "/transactions/history"})
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getUserTransactionHistory(
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Date filter: 7days, 15days, 30days, 3m, 6m, year", example = "30days") @RequestParam(defaultValue = "30days") String period,
            @Parameter(description = "Transaction type: WALLET, CARD, WALLET_TOPUP or ALL", example = "WALLET_TOPUP") @RequestParam(required = false) String type,
            @Parameter(description = "Transaction status: PENDING, COMPLETED, FAILED", example = "COMPLETED") @RequestParam(required = false) String status,
            @Parameter(description = "Search by eSewa ID", example = "txn_123") @RequestParam(required = false) String search,
            @Parameter(description = "Custom start date (ISO 8601, optional)", example = "2026-04-01T00:00:00") @RequestParam(required = false) LocalDateTime fromDate,
            @Parameter(description = "Custom end date (ISO 8601, optional)", example = "2026-04-11T23:59:59") @RequestParam(required = false) LocalDateTime toDate,
            Authentication authentication,
            HttpServletRequest request) {
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();

        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Page must be >= 0 and size must be > 0");
        }

        DateRange dateRange = resolveBillingDateRange(period, fromDate, toDate);
        TransactionType transactionType = parseTransactionType(type);
        TransactionStatus transactionStatus = parseTransactionStatus(status);

        Page<Transaction> transactionPage = transactionService.getUserTransactionsByFilters(
                user.getUserId(),
                transactionType,
                transactionStatus,
                search,
                dateRange.from(),
                dateRange.to(),
                page,
                size
        );

        Page<TransactionResponse> responsePage = transactionPage.map(t -> TransactionResponse.builder()
                .transactionId(t.getTransactionId())
                .userId(t.getUser().getUserId())
                .userName(t.getUser().getFullName())
                .amount(t.getAmount())
                .type(t.getType())
                .status(t.getStatus())
                .esewaTransactionId(t.getEsewaTransactionId())
                .relatedTaskId(t.getRelatedTask() != null ? t.getRelatedTask().getTaskId() : null)
                .completedAt(t.getCompletedAt())
                .createdAt(t.getCreatedAt())
                .build());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Transaction history retrieved successfully", responsePage, request));
    }

    private TransactionType parseTransactionType(String type) {
        if (type == null || type.trim().isEmpty() || "all".equalsIgnoreCase(type.trim())) {
            return null;
        }

        try {
            return TransactionType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid type filter. Allowed values: ALL, WALLET, CARD, WALLET_TOPUP");
        }
    }

    private TransactionStatus parseTransactionStatus(String status) {
        if (status == null || status.trim().isEmpty() || "all".equalsIgnoreCase(status.trim())) {
            return null;
        }

        try {
            return TransactionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid status filter. Allowed values: ALL, PENDING, COMPLETED, FAILED");
        }
    }

    private DateRange resolveBillingDateRange(String period, LocalDateTime fromDate, LocalDateTime toDate) {
        if (fromDate != null && toDate != null) {
            return new DateRange(fromDate, toDate, "custom");
        }

        LocalDateTime now = LocalDateTime.now();
        String normalized = period == null ? "30days" : period.trim().toLowerCase();
        LocalDateTime from;

        switch (normalized) {
            case "7days", "7day" -> normalized = "7days";
            case "15days", "15day" -> normalized = "15days";
            case "30days", "30day", "month" -> normalized = "30days";
            case "3m", "3month", "3months" -> normalized = "3m";
            case "6m", "6month", "6months" -> normalized = "6m";
            case "year", "1y", "12m" -> normalized = "year";
            default -> normalized = "30days";
        }

        from = switch (normalized) {
            case "7days" -> now.minusDays(7);
            case "15days" -> now.minusDays(15);
            case "3m" -> now.minusMonths(3);
            case "6m" -> now.minusMonths(6);
            case "year" -> now.minusYears(1);
            default -> now.minusDays(30);
        };

        return new DateRange(from, now, normalized);
    }

    private record DateRange(LocalDateTime from, LocalDateTime to, String period) {
    }

}
