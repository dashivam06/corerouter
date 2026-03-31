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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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
        CompletableFuture<Map<String, Object>> redisFuture = safeAsync("redis", healthCheckService::checkRedis, 5);
        CompletableFuture<Map<String, Object>> vllmFuture = safeAsync("vllm", healthCheckService::checkVllm, 5);
        CompletableFuture<Map<String, Object>> dbFuture = safeAsync("database", healthCheckService::checkDatabase, 5);
        CompletableFuture<Map<String, Object>> workerFuture = safeAsync("worker", healthCheckService::checkWorkers, 5);

        CompletableFuture.allOf(redisFuture, vllmFuture, dbFuture, workerFuture).join();

        Map<String, Object> payload = Map.of(
                "redis", redisFuture.join(),
                "vllm", vllmFuture.join(),
                "database", dbFuture.join(),
                "worker", workerFuture.join()
        );

                List<String> downCriticalComponents = new ArrayList<>();
                if (isDown(payload.get("redis"))) {
                        downCriticalComponents.add("redis");
                }
                if (isDown(payload.get("vllm"))) {
                        downCriticalComponents.add("vllm");
                }
                if (isDown(payload.get("database"))) {
                        downCriticalComponents.add("database");
                }

                if (!downCriticalComponents.isEmpty()) {
                        ApiResponse<Map<String, Object>> errorResponse = ApiResponse.<Map<String, Object>>builder()
                                        .timestamp(LocalDateTime.now())
                                        .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                                        .success(false)
                                        .message("Critical components down: " + String.join(", ", downCriticalComponents))
                                        .path(request.getRequestURI())
                                        .method(request.getMethod())
                                        .data(payload)
                                        .build();
                        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
                }

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Health check completed", payload, request));
    }

        @SuppressWarnings("unchecked")
        private boolean isDown(Object componentResult) {
                if (!(componentResult instanceof Map<?, ?> rawMap)) {
                        return true;
                }
                Object status = ((Map<String, Object>) rawMap).get("status");
                return status == null || "DOWN".equalsIgnoreCase(String.valueOf(status));
        }

    private CompletableFuture<Map<String, Object>> safeAsync(String name, Supplier<Map<String, Object>> supplier, long timeoutSeconds) {
        return CompletableFuture
                .supplyAsync(supplier)
                .completeOnTimeout(timeoutResult(), timeoutSeconds, TimeUnit.SECONDS)
                .exceptionally(ex -> Map.of(
                        "status", "DOWN",
                        "reason", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage(),
                        "component", name
                ));
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

        @GetMapping("/total-requests")
        public ResponseEntity<ApiResponse<Object>> getTotalRequests(
                        @RequestParam(required = false, defaultValue = "1d") String range,
                        HttpServletRequest request) {

                Object payload = azureInsightsService.getTotalRequests(range);
                return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Total requests fetched successfully", payload, request));
        }

        @GetMapping("/failed-requests")
        public ResponseEntity<ApiResponse<Object>> getFailedRequests(
                        @RequestParam(required = false, defaultValue = "1d") String range,
                        HttpServletRequest request) {

                Object payload = azureInsightsService.getFailedRequests(range);
                return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Failed requests fetched successfully", payload, request));
        }

        @GetMapping("/error-rate")
        public ResponseEntity<ApiResponse<Object>> getErrorRate(
                        @RequestParam(required = false, defaultValue = "1d") String range,
                        HttpServletRequest request) {

                Object payload = azureInsightsService.getErrorRate(range);
                return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Error rate fetched successfully", payload, request));
        }

        @GetMapping("/average-response-time")
        public ResponseEntity<ApiResponse<Object>> getAverageResponseTime(
                        @RequestParam(required = false, defaultValue = "1d") String range,
                        HttpServletRequest request) {

                Object payload = azureInsightsService.getAverageResponseTime(range);
                return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Average response time fetched successfully", payload, request));
        }

        @GetMapping("/requests-over-time")
        public ResponseEntity<ApiResponse<Object>> getRequestsOverTime(
                        @RequestParam(required = false, defaultValue = "1d") String range,
                        HttpServletRequest request) {

                Object payload = azureInsightsService.getRequestsOverTime(range);
                return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Requests over time fetched successfully", payload, request));
        }

        @GetMapping("/failed-requests-over-time")
        public ResponseEntity<ApiResponse<Object>> getFailedRequestsOverTime(
                        @RequestParam(required = false, defaultValue = "1d") String range,
                        HttpServletRequest request) {

                Object payload = azureInsightsService.getFailedRequestsOverTime(range);
                return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Failed requests over time fetched successfully", payload, request));
        }

        @GetMapping("/top-endpoints")
        public ResponseEntity<ApiResponse<Object>> getTopEndpoints(
                        @RequestParam(required = false, defaultValue = "1d") String range,
                        HttpServletRequest request) {

                Object payload = azureInsightsService.getTopEndpointsForRange(range);
                return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Top endpoints fetched successfully", payload, request));
        }

    private Map<String, Object> timeoutResult() {
        return Map.of(
                "status", "DOWN",
                "reason", "Timeout after 5 seconds"
        );
    }
}
