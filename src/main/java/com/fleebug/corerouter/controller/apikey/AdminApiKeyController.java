package com.fleebug.corerouter.controller.apikey;

import com.fleebug.corerouter.dto.apikey.response.AdminApiKeyInsightsResponse;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.service.apikey.ApiKeyService;
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
}
