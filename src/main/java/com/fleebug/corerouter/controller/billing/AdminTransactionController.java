package com.fleebug.corerouter.controller.billing;

import com.fleebug.corerouter.dto.billing.response.EarningsDataResponse;
import com.fleebug.corerouter.dto.billing.response.TransactionResponse;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.entity.payment.Transaction;
import com.fleebug.corerouter.enums.payment.TransactionStatus;
import com.fleebug.corerouter.enums.payment.TransactionType;
import com.fleebug.corerouter.service.payment.TransactionService;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/transactions")
@RequiredArgsConstructor
@Tag(name = "Admin Transactions", description = "Transaction analytics and history (ADMIN only)")
public class AdminTransactionController {

    private final TransactionService transactionService;
    private final TelemetryClient telemetryClient;

    @Operation(
            summary = "Get daily earnings",
            description = "Get daily earnings (top-up income) aggregated from all users. Uses UTC time boundaries."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Daily earnings retrieved successfully")
    })
    @GetMapping("/earnings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EarningsDataResponse>> getDailyEarnings(
            @Parameter(description = "Filter period: 'today' or 'all'", example = "today")
            @RequestParam(defaultValue = "all") String filterPeriod,
            @Parameter(description = "Custom start date (ISO 8601 UTC, optional)", example = "2026-04-01T00:00:00")
            @RequestParam(required = false) LocalDateTime fromDate,
            @Parameter(description = "Custom end date (ISO 8601 UTC, optional)", example = "2026-04-05T23:59:59")
            @RequestParam(required = false) LocalDateTime toDate,
            HttpServletRequest request) {

        LocalDateTime nowUtc = LocalDateTime.now(Clock.systemUTC());
        LocalDateTime from = fromDate;
        LocalDateTime to = toDate;
        String appliedFilter = filterPeriod;

        if (from == null || to == null) {
            if ("today".equalsIgnoreCase(filterPeriod)) {
                from = LocalDate.now(ZoneOffset.UTC).atStartOfDay();
                to = nowUtc;
            } else {
                from = nowUtc.minusDays(90);
                to = nowUtc;
            }
        }

        Map<String, String> properties = new HashMap<>();
        properties.put("filterPeriod", appliedFilter);
        telemetryClient.trackTrace("Admin transactions: get daily earnings", SeverityLevel.Information, properties);

        List<Object[]> dailyEarnings = transactionService.getDailyEarnings(from, to);

        Map<String, String> earningsByDate = new LinkedHashMap<>();
        BigDecimal totalEarned = BigDecimal.ZERO;
        int totalTransactionCount = 0;

        for (Object[] row : dailyEarnings) {
            LocalDate date = (LocalDate) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            Long count = (Long) row[2];

            earningsByDate.put(date.toString(), amount.setScale(2, RoundingMode.HALF_UP).toString());
            totalEarned = totalEarned.add(amount);
            totalTransactionCount += count.intValue();
        }

        EarningsDataResponse response = EarningsDataResponse.builder()
                .earningsByDate(earningsByDate)
                .totalEarnings(totalEarned.setScale(2, RoundingMode.HALF_UP).toString())
                .totalTransactionCount(totalTransactionCount)
                .filterPeriod(appliedFilter)
                .filterType("WALLET_TOPUP")
                .build();

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Daily earnings retrieved successfully", response, request));
    }

    @Operation(
            summary = "Get paginated transaction history",
            description = "Get paginated transaction history with UTC date filter, type/status filter and search by user/email/eSewa ID"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactionHistory(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Date filter: today or all", example = "today")
            @RequestParam(defaultValue = "all") String dateFilter,
            @Parameter(description = "Transaction type: WALLET, CARD, WALLET_TOPUP", example = "WALLET_TOPUP")
            @RequestParam(required = false) String type,
            @Parameter(description = "Transaction status: PENDING, COMPLETED, FAILED", example = "COMPLETED")
            @RequestParam(required = false) String status,
            @Parameter(description = "Search by user name, email or eSewa ID", example = "jane@company.com")
            @RequestParam(required = false) String search,
            @Parameter(description = "Custom start date (ISO 8601 UTC, optional)", example = "2026-04-01T00:00:00")
            @RequestParam(required = false) LocalDateTime fromDate,
            @Parameter(description = "Custom end date (ISO 8601 UTC, optional)", example = "2026-04-05T23:59:59")
            @RequestParam(required = false) LocalDateTime toDate,
            HttpServletRequest request) {

        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Page must be >= 0 and size must be > 0");
        }

        LocalDateTime nowUtc = LocalDateTime.now(Clock.systemUTC());
        LocalDateTime from = fromDate;
        LocalDateTime to = toDate;

        if (from == null || to == null) {
            if ("today".equalsIgnoreCase(dateFilter)) {
                from = LocalDate.now(ZoneOffset.UTC).atStartOfDay();
                to = nowUtc;
            } else {
                from = nowUtc.minusDays(90);
                to = nowUtc;
            }
        }

        TransactionType transactionType = parseTransactionType(type);
        TransactionStatus transactionStatus = parseTransactionStatus(status);

        Page<Transaction> transactionPage = transactionService.getTransactionsByFilters(
                transactionType,
                transactionStatus,
                search,
                from,
                to,
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
        if (type == null || type.trim().isEmpty()) {
            return null;
        }

        try {
            return TransactionType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid type filter. Allowed values: WALLET, CARD, WALLET_TOPUP");
        }
    }

    private TransactionStatus parseTransactionStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }

        try {
            return TransactionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid status filter. Allowed values: PENDING, COMPLETED, FAILED");
        }
    }
}
