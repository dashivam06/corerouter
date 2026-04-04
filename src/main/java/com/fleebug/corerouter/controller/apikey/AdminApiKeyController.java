package com.fleebug.corerouter.controller.apikey;

import com.fleebug.corerouter.dto.apikey.response.AdminApiKeyInsightsResponse;
import com.fleebug.corerouter.dto.apikey.response.ApiKeyAnalyticsResponse;
import com.fleebug.corerouter.dto.apikey.response.ApiKeyResponse;
import com.fleebug.corerouter.dto.apikey.response.PaginatedApiKeyListResponse;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.enums.apikey.ApiKeyStatus;
import com.fleebug.corerouter.service.apikey.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/apikeys")
@RequiredArgsConstructor
@Tag(name = "Admin API Keys", description = "Admin API key insights")
public class AdminApiKeyController {

    private final ApiKeyService apiKeyService;

    @Operation(summary = "Get API key insights", description = "Get total keys and status-wise API key counts for admin dashboard")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "API key insights retrieved successfully")
    })
    @GetMapping("/insights")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminApiKeyInsightsResponse>> getApiKeyInsights(HttpServletRequest request) {
        AdminApiKeyInsightsResponse insights = apiKeyService.getAdminInsights();

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "API key insights retrieved successfully", insights, request));
    }

    @Operation(summary = "Get API key analytics by date range", description = "Get daily API key creation and revocation counts for a date range")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "API key analytics retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range")
    })
    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ApiKeyAnalyticsResponse>> getApiKeyAnalytics(
            @Parameter(description = "Start date (ISO 8601)", example = "2026-03-26T00:00:00")
            @RequestParam java.time.LocalDateTime from,
            @Parameter(description = "End date (ISO 8601)", example = "2026-04-04T23:59:59")
            @RequestParam java.time.LocalDateTime to,
            HttpServletRequest request) {

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        ApiKeyAnalyticsResponse analytics = apiKeyService.getApiKeyAnalyticsByDateRange(from, to);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "API key analytics retrieved successfully", analytics, request));
    }

    @Operation(summary = "Get paginated API key list", description = "Get paginated API key list with optional status filter")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "API keys retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    })
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaginatedApiKeyListResponse>> getApiKeysList(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by API key status (ACTIVE, INACTIVE, REVOKED)", example = "ACTIVE")
            @RequestParam(required = false) ApiKeyStatus status,
            HttpServletRequest request) {

        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Page must be >= 0 and size must be > 0");
        }

        PaginatedApiKeyListResponse response = apiKeyService.getApiKeysWithFilters(page, size, status);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "API keys retrieved successfully", response, request));
    }

    @Operation(summary = "Update API key status", description = "Admin can change status of any API key by API key ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "API key status updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "API key not found")
    })
    @PatchMapping("/{apiKeyId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> updateApiKeyStatus(
            @Parameter(description = "API key ID", example = "1") @PathVariable Integer apiKeyId,
            @Parameter(description = "New API key status", example = "INACTIVE") @RequestParam ApiKeyStatus status,
            HttpServletRequest request) {

        ApiKeyResponse response = apiKeyService.updateApiKeyStatusByAdmin(apiKeyId, status);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "API key status updated successfully", response, request));
    }
}
