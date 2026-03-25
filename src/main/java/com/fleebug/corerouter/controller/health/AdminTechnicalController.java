package com.fleebug.corerouter.controller.health;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.service.health.AzureInsightsService;
import com.fleebug.corerouter.service.health.HealthCheckService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/admin/technical")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Technical", description = "Technical health and monitoring")
public class AdminTechnicalController {

    private final HealthCheckService healthCheckService;
    private final AzureInsightsService azureInsightsService;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealth(HttpServletRequest request) {
        CompletableFuture<Map<String, Object>> redisFuture = CompletableFuture
                .supplyAsync(healthCheckService::checkRedis)
                .completeOnTimeout(timeoutResult(), 7, TimeUnit.SECONDS);

        CompletableFuture<Map<String, Object>> vllmFuture = CompletableFuture
                .supplyAsync(healthCheckService::checkVllm)
                .completeOnTimeout(timeoutResult(), 5, TimeUnit.SECONDS);

        CompletableFuture<Map<String, Object>> dbFuture = CompletableFuture
                .supplyAsync(healthCheckService::checkDatabase)
                .completeOnTimeout(timeoutResult(), 5, TimeUnit.SECONDS);

        CompletableFuture<Map<String, Object>> workerFuture = CompletableFuture
                .supplyAsync(healthCheckService::checkWorkers)
                .completeOnTimeout(timeoutResult(), 5, TimeUnit.SECONDS);

        CompletableFuture.allOf(redisFuture, vllmFuture, dbFuture, workerFuture).join();

        Map<String, Object> payload = Map.of(
                "redis", redisFuture.join(),
                "vllm", vllmFuture.join(),
                "database", dbFuture.join(),
                "worker", workerFuture.join()
        );

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Health check completed", payload, request));
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            HttpServletRequest request) {

        Map<String, Object> payload = Map.of(
                "requestStats", azureInsightsService.getRequestStats(from, to),
                "failedJobs", azureInsightsService.getFailedJobs(from, to),
                "topEndpoints", azureInsightsService.getTopEndpoints(from, to),
                "trafficByHour", azureInsightsService.getTrafficByHour(from, to),
                "alerts", azureInsightsService.getAlerts(from, to)
        );

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Overview fetched successfully", payload, request));
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Object>> getLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "ALL") String severity,
                        @RequestParam(required = false, defaultValue = "100") Integer pageSize,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursorTimestamp,
                        @RequestParam(required = false) String cursorItemId,
            HttpServletRequest request) {

                Object payload = azureInsightsService.getLogsPaged(from, to, severity, pageSize, cursorTimestamp, cursorItemId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Logs fetched successfully", payload, request));
    }

    @GetMapping("/errors")
    public ResponseEntity<ApiResponse<Object>> getErrors(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                        @RequestParam(required = false, defaultValue = "50") Integer pageSize,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursorTimestamp,
                        @RequestParam(required = false) String cursorItemId,
            HttpServletRequest request) {

                Object payload = azureInsightsService.getErrorsPaged(from, to, pageSize, cursorTimestamp, cursorItemId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Errors fetched successfully", payload, request));
    }

    @GetMapping("/warnings")
    public ResponseEntity<ApiResponse<Object>> getWarnings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                        @RequestParam(required = false, defaultValue = "50") Integer pageSize,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursorTimestamp,
                        @RequestParam(required = false) String cursorItemId,
            HttpServletRequest request) {

                Object payload = azureInsightsService.getWarningsPaged(from, to, pageSize, cursorTimestamp, cursorItemId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Warnings fetched successfully", payload, request));
    }

    @GetMapping("/failed-jobs")
    public ResponseEntity<ApiResponse<Object>> getFailedJobs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                        @RequestParam(required = false, defaultValue = "50") Integer pageSize,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursorTimestamp,
                        @RequestParam(required = false) String cursorItemId,
            HttpServletRequest request) {

                Object payload = azureInsightsService.getFailedJobsPaged(from, to, pageSize, cursorTimestamp, cursorItemId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Failed jobs fetched successfully", payload, request));
    }

    private Map<String, Object> timeoutResult() {
        return Map.of(
                "status", "DOWN",
                "reason", "Timeout after 5 seconds"
        );
    }
}
