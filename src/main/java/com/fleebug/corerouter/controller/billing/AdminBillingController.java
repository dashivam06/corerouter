package com.fleebug.corerouter.controller.billing;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.fleebug.corerouter.dto.billing.request.CreateBillingConfigRequest;
import com.fleebug.corerouter.dto.billing.request.RecordUsageRequest;
import com.fleebug.corerouter.dto.billing.request.UpdateBillingConfigRequest;
import com.fleebug.corerouter.dto.billing.response.BillingConfigResponse;
import com.fleebug.corerouter.dto.billing.response.BillingInsightsResponse;
import com.fleebug.corerouter.dto.billing.response.UsageRecordResponse;
import com.fleebug.corerouter.dto.billing.response.UsageSummaryResponse;
import com.fleebug.corerouter.dto.billing.response.EarningsDataResponse;
import com.fleebug.corerouter.dto.billing.response.TransactionResponse;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.entity.payment.Transaction;
import com.fleebug.corerouter.enums.payment.TransactionStatus;
import com.fleebug.corerouter.enums.payment.TransactionType;
import com.fleebug.corerouter.service.billing.BillingConfigService;
import com.fleebug.corerouter.service.billing.UsageService;
import com.fleebug.corerouter.service.payment.TransactionService;
import com.fleebug.corerouter.repository.user.UserRepository;
import com.fleebug.corerouter.enums.user.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/billing")
@RequiredArgsConstructor
@Tag(name = "Admin Billing", description = "Billing configuration and usage management (ADMIN only)")
public class AdminBillingController {

    private final BillingConfigService billingConfigService;
    private final UsageService usageService;
    private final TransactionService transactionService;
    private final UserRepository userRepository;
    private final TelemetryClient telemetryClient;

    // ---- Billing Config CRUD ----

    /**
     * Create a new billing configuration for a model.
     *
     * @param createRequest billing config creation payload
     * @param request       HTTP servlet request
     * @return created billing config
     */
    @Operation(summary = "Create billing config", description = "Create a new billing configuration for a model with pricing type and metadata")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Billing config created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or billing config already exists for model"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Model not found")
    })
    @PostMapping("/configs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BillingConfigResponse>> createBillingConfig(
            @Valid @RequestBody CreateBillingConfigRequest createRequest,
            HttpServletRequest request) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("modelId", String.valueOf(createRequest.getModelId()));
        telemetryClient.trackTrace("Create billing config request", SeverityLevel.Information, properties);

        BillingConfigResponse response = billingConfigService.createBillingConfig(createRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Billing config created successfully", response, request));
    }




     // ---- Earnings & Transactions (Admin) ----

    /**
     * Get daily earnings aggregated from all users.
     * Shows the money earned from wallet top-ups grouped by date.
     *
     * @param filterPeriod "today" or "all" - determines date range
     * @param fromDate     optional custom start date (ISO 8601)
     * @param toDate       optional custom end date (ISO 8601)
     * @param request      HTTP servlet request
     * @return earnings data grouped by date with counts
     */
    @Operation(
        summary = "Get daily earnings",
        description = "Get daily earnings (top-up income) aggregated from all users. Supports filtering by period (today/all) or custom date range."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Daily earnings retrieved successfully")
    })
    @GetMapping("/earnings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EarningsDataResponse>> getDailyEarnings(
            @Parameter(description = "Filter period: 'today' or 'all'", example = "today") 
            @RequestParam(defaultValue = "all") String filterPeriod,
            @Parameter(description = "Custom start date (ISO 8601, optional)", example = "2026-04-01T00:00:00") 
            @RequestParam(required = false) LocalDateTime fromDate,
            @Parameter(description = "Custom end date (ISO 8601, optional)", example = "2026-04-05T23:59:59") 
            @RequestParam(required = false) LocalDateTime toDate,
            HttpServletRequest request) {

        LocalDateTime from = fromDate;
        LocalDateTime to = toDate;
        String appliedFilter = filterPeriod;

        // If custom dates not provided, use filter period
        if (from == null || to == null) {
            LocalDateTime now = LocalDateTime.now();
            if ("today".equalsIgnoreCase(filterPeriod)) {
                from = now.toLocalDate().atStartOfDay();
                to = now.toLocalDate().plusDays(1).atStartOfDay();
            } else {
                // "all" - get last 90 days by default for performance
                from = now.minusDays(90);
                to = now;
            }
        }

        Map<String, String> properties = new HashMap<>();
        properties.put("filterPeriod", appliedFilter);
        telemetryClient.trackTrace("Admin: get daily earnings", SeverityLevel.Information, properties);

        // Get daily earnings from repository
        List<Object[]> dailyEarnings = transactionService.getDailyEarnings(from, to);

        // Convert to map format: date -> amount
        Map<String, String> earningsByDate = new LinkedHashMap<>();
        BigDecimal totalEarned = BigDecimal.ZERO;
        int totalTransactionCount = 0;

        for (Object[] row : dailyEarnings) {
            LocalDate date = (LocalDate) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            Long count = (Long) row[2];

            earningsByDate.put(date.toString(), amount.setScale(2, java.math.RoundingMode.HALF_UP).toString());
            totalEarned = totalEarned.add(amount);
            totalTransactionCount += count.intValue();
        }

        EarningsDataResponse response = EarningsDataResponse.builder()
                .earningsByDate(earningsByDate)
                .totalEarnings(totalEarned.setScale(2, java.math.RoundingMode.HALF_UP).toString())
                .totalTransactionCount(totalTransactionCount)
                .filterPeriod(appliedFilter)
                .filterType("WALLET_TOPUP")
                .build();

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Daily earnings retrieved successfully", response, request));
    }

    /**
     * Get transaction history with filtering options.
     * Supports filtering by type, status, and date range.
     *
     * @param transactionType "WALLET", "CARD", "WALLET_TOPUP", or null for all
     * @param status         "PENDING", "COMPLETED", "FAILED", or null for all
     * @param filterPeriod   "today" or "all"
     * @param fromDate       optional custom start date
     * @param toDate         optional custom end date
     * @param request        HTTP servlet request
     * @return list of transactions matching filters
     */
    @Operation(
        summary = "Get transaction history",
        description = "Get transaction history with filtering by type, status, and date range"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully")
    })
    @GetMapping("/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionHistory(
            @Parameter(description = "Transaction type filter: WALLET, CARD, WALLET_TOPUP, or empty for all", example = "WALLET_TOPUP") 
            @RequestParam(required = false) String transactionType,
            @Parameter(description = "Transaction type alias used by frontend: WALLET, CARD, WALLET_TOPUP, Wallet Topup", example = "Wallet Topup")
            @RequestParam(required = false) String type,
            @Parameter(description = "Transaction status filter: PENDING, COMPLETED, FAILED, or empty for all", example = "COMPLETED") 
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter period: 'today' or 'all'", example = "today") 
            @RequestParam(defaultValue = "all") String filterPeriod,
            @Parameter(description = "Date filter alias used by frontend: 'today' or 'all'", example = "today")
            @RequestParam(required = false) String dateFilter,
            @Parameter(description = "Search by user name, email, or eSewa ID", example = "john@example.com")
            @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "1000")
            @RequestParam(defaultValue = "1000") int size,
            @Parameter(description = "Custom start date (ISO 8601, optional)", example = "2026-04-01T00:00:00") 
            @RequestParam(required = false) LocalDateTime fromDate,
            @Parameter(description = "Custom end date (ISO 8601, optional)", example = "2026-04-05T23:59:59") 
            @RequestParam(required = false) LocalDateTime toDate,
            HttpServletRequest request) {

        String resolvedFilterPeriod = (dateFilter != null && !dateFilter.isBlank()) ? dateFilter : filterPeriod;
        String resolvedType = (transactionType != null && !transactionType.isBlank()) ? transactionType : type;

        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 1000;
        }

        LocalDateTime from = fromDate;
        LocalDateTime to = toDate;

        // If custom dates not provided, use filter period
        if (from == null || to == null) {
            LocalDateTime now = LocalDateTime.now();
            if ("today".equalsIgnoreCase(resolvedFilterPeriod)) {
                from = now.toLocalDate().atStartOfDay();
                to = now.toLocalDate().plusDays(1).atStartOfDay();
            } else {
                // "all" - get last 90 days by default
                from = now.minusDays(90);
                to = now;
            }
        }

        Map<String, String> properties = new HashMap<>();
        properties.put("filterPeriod", resolvedFilterPeriod);
        if (resolvedType != null && !resolvedType.isBlank()) {
            properties.put("type", resolvedType);
        }
        if (status != null && !status.isBlank()) {
            properties.put("status", status);
        }
        if (search != null && !search.isBlank()) {
            properties.put("search", search);
        }
        telemetryClient.trackTrace("Admin: get transaction history", SeverityLevel.Information, properties);

        TransactionType transactionTypeEnum = null;
        TransactionStatus txStatus = null;

        if (resolvedType != null && !resolvedType.isEmpty()) {
            try {
                String normalizedType = resolvedType.trim().toUpperCase().replace('-', '_').replace(' ', '_');
                transactionTypeEnum = TransactionType.valueOf(normalizedType);
            } catch (IllegalArgumentException ignored) {
                // Keep null to avoid breaking old clients on invalid filter values.
            }
        }

        if (status != null && !status.isEmpty()) {
            try {
                txStatus = TransactionStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Keep null to avoid breaking old clients on invalid filter values.
            }
        }

        Page<Transaction> transactionPage = transactionService.getTransactionsByFilters(
            transactionTypeEnum,
                txStatus,
            search,
                from,
                to,
            page,
            size
        );

        List<Transaction> transactions = transactionPage.getContent();

        List<TransactionResponse> transactionResponses = transactions.stream()
                .map(t -> TransactionResponse.builder()
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
                        .build())
                .toList();

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Transaction history retrieved successfully", transactionResponses, request));
    }

    /**
     * Get all billing configurations.
     *
     * @param request HTTP servlet request
     * @return list of all billing configs
     */
    @Operation(summary = "Get all billing configs", description = "Retrieve all billing configurations across all models")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Billing configs retrieved successfully")
    })
    @GetMapping("/configs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<BillingConfigResponse>>> getAllBillingConfigs(
            HttpServletRequest request) {
        
        // telemetryClient.trackTrace("Get all billing configs request", SeverityLevel.Verbose, null);

        List<BillingConfigResponse> configs = billingConfigService.getAllBillingConfigs();

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Billing configs retrieved successfully", configs, request));
    }

    /**
     * Get a billing configuration by its ID.
     *
     * @param billingId billing config ID
     * @param request   HTTP servlet request
     * @return billing config
     */
    @Operation(summary = "Get billing config by ID", description = "Retrieve a billing configuration by its ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Billing config retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Billing config not found")
    })
    @GetMapping("/configs/{billingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BillingConfigResponse>> getBillingConfigById(
            @Parameter(description = "Billing config ID", example = "1") @PathVariable Integer billingId,
            HttpServletRequest request) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("billingId", String.valueOf(billingId));
        // telemetryClient.trackTrace("Get billing config by ID", SeverityLevel.Verbose, properties);

        BillingConfigResponse response = billingConfigService.getBillingConfigById(billingId);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Billing config retrieved successfully", response, request));
    }

    /**
     * Get a billing configuration by model ID.
     *
     * @param modelId model ID
     * @param request HTTP servlet request
     * @return billing config for the model
     */
    @Operation(summary = "Get billing config by model", description = "Retrieve the billing configuration associated with a specific model")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Billing config retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Billing config not found for model")
    })
    @GetMapping({"/configs/model/{modelId}"})
    @PreAuthorize("hasAnyRole('ADMIN','WORKER')")
    public ResponseEntity<ApiResponse<BillingConfigResponse>> getBillingConfigByModelId(
            @Parameter(description = "Model ID", example = "5") @PathVariable Integer modelId,
            HttpServletRequest request) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("modelId", String.valueOf(modelId));
        // telemetryClient.trackTrace("Get billing config for model", SeverityLevel.Verbose, properties);

        BillingConfigResponse response = billingConfigService.getBillingConfigByModelId(modelId);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Billing config retrieved successfully", response, request));
    }

    /**
     * Update an existing billing configuration.
     *
     * @param billingId     billing config ID
     * @param updateRequest update payload
     * @param request       HTTP servlet request
     * @return updated billing config
     */
    @Operation(summary = "Update billing config", description = "Update pricing type and/or metadata of an existing billing configuration")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Billing config updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Billing config not found")
    })
    @PutMapping("/configs/{billingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BillingConfigResponse>> updateBillingConfig(
            @Parameter(description = "Billing config ID", example = "1") @PathVariable Integer billingId,
            @Valid @RequestBody UpdateBillingConfigRequest updateRequest,
            HttpServletRequest request) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("billingId", String.valueOf(billingId));
        telemetryClient.trackTrace("Update billing config", SeverityLevel.Information, properties);

        BillingConfigResponse response = billingConfigService.updateBillingConfig(billingId, updateRequest);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Billing config updated successfully", response, request));
    }

    /**
     * Delete a billing configuration.
     *
     * @param billingId billing config ID
     * @param request   HTTP servlet request
     * @return empty response
     */
    @Operation(summary = "Delete billing config", description = "Soft delete a billing configuration while preserving historical usage integrity")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Billing config deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Billing config not found")
    })
    @DeleteMapping("/configs/{billingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBillingConfig(
            @Parameter(description = "Billing config ID", example = "1") @PathVariable Integer billingId,
            HttpServletRequest request) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("billingId", String.valueOf(billingId));
        telemetryClient.trackTrace("Delete billing config", SeverityLevel.Information, properties);

        billingConfigService.deleteBillingConfig(billingId);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Billing config soft deleted successfully", null, request));
    }

    // ---- Usage Recording (called internally or by admin) ----

    /**
     * Record usage for a completed task.
     *
     * @param recordRequest usage recording payload
     * @param request       HTTP servlet request
     * @return recorded usage
     */
    @Operation(summary = "Record usage", description = "Record usage for a completed task. A task can have multiple usage records (e.g. input tokens + output tokens).")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Usage recorded successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or duplicate unit type for task"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Task not found or billing config missing")
    })
    @PostMapping("/usage")
    @PreAuthorize("hasAnyRole('ADMIN','WORKER')")
    public ResponseEntity<ApiResponse<UsageRecordResponse>> recordUsage(
            @Valid @RequestBody RecordUsageRequest recordRequest,
            HttpServletRequest request) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("taskId", recordRequest.getTaskId());
        telemetryClient.trackTrace("Record usage request", SeverityLevel.Information, properties);

        UsageRecordResponse response = usageService.recordUsage(recordRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Usage recorded successfully", response, request));
    }

    // ---- Usage Queries (admin) ----

    /**
     * Get all usage records for a task.
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UsageRecordResponse>>> getUsageByTask(
            @Parameter(description = "Task ID", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479") @PathVariable String taskId,
            HttpServletRequest request) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("taskId", taskId);
        // telemetryClient.trackTrace("Get usage by taskId", SeverityLevel.Verbose, properties);

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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UsageSummaryResponse>> getUsageSummaryByApiKey(
            @Parameter(description = "API key ID", example = "10") @PathVariable Integer apiKeyId,
            @Parameter(description = "Period start (ISO 8601)", example = "2026-03-01T00:00:00") @RequestParam LocalDateTime from,
            @Parameter(description = "Period end (ISO 8601)", example = "2026-03-31T23:59:59") @RequestParam LocalDateTime to,
            HttpServletRequest request) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("apiKeyId", String.valueOf(apiKeyId));
        // telemetryClient.trackTrace("Get usage summary for apiKey", SeverityLevel.Verbose, properties);

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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UsageSummaryResponse>> getUsageSummaryByModel(
            @Parameter(description = "API key ID", example = "10") @PathVariable Integer apiKeyId,
            @Parameter(description = "Period start (ISO 8601)", example = "2026-03-01T00:00:00") @RequestParam LocalDateTime from,
            @Parameter(description = "Period end (ISO 8601)", example = "2026-03-31T23:59:59") @RequestParam LocalDateTime to,
            HttpServletRequest request) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("apiKeyId", String.valueOf(apiKeyId));
        // telemetryClient.trackTrace("Get usage summary by model for apiKey", SeverityLevel.Verbose, properties);

        UsageSummaryResponse response = usageService.getUsageSummaryByApiKeyGroupedByModel(apiKeyId, from, to);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Usage summary by model retrieved successfully", response, request));
    }

    @Operation(summary = "Get billing insights", description = "Get total system balance, this month's billing volume, and today's top-up amount across all users")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Billing insights retrieved successfully")
    })
    @GetMapping("/insights")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BillingInsightsResponse>> getBillingInsights(HttpServletRequest request) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();

        BigDecimal totalBalance = userRepository.sumAllBalancesByRole(UserRole.USER);
        BigDecimal thisMonthVolume = transactionService.getTopUpAmountByPeriod(monthStart, now);
        BigDecimal todayTopUpAmount = transactionService.getTopUpAmountByPeriod(todayStart, now);

        BillingInsightsResponse insights = BillingInsightsResponse.builder()
                .totalBalance(totalBalance)
                .thisMonthVolume(thisMonthVolume)
                .todayTopUpAmount(todayTopUpAmount)
                .build();

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Billing insights retrieved successfully", insights, request));
    }
}
