package com.fleebug.corerouter.controller.apikey;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.apikey.request.CreateApiKeyRequest;
import com.fleebug.corerouter.dto.apikey.request.UpdateApiKeyRequest;
import com.fleebug.corerouter.dto.apikey.response.ApiKeyResponse;
import com.fleebug.corerouter.service.apikey.ApiKeyService;
import com.fleebug.corerouter.security.details.CustomUserDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/apikeys")
@RequiredArgsConstructor
@Tag(name = "API Keys", description = "API key management — generate, view, update, enable/disable, and delete API keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final TelemetryClient telemetryClient;

    /**
     * Generate a new API key
     *
     * @param createRequest API key creation request
     * @param authentication Authenticated user
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse containing created API key
     */
    @Operation(summary = "Generate API key", description = "Generate a new API key for the authenticated user with optional description and rate limits")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "API key generated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> generateApiKey(
            @Valid @RequestBody CreateApiKeyRequest createRequest,
            Authentication authentication,
            HttpServletRequest request) {
        
        telemetryClient.trackTrace("Generate API key request received", SeverityLevel.Verbose, null);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        ApiKeyResponse apiKeyResponse = apiKeyService.generateApiKey(userDetails.getUser(), createRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "API key generated successfully", apiKeyResponse, request));
    }

    /**
     * Get all API keys for the authenticated user
     *
     * @param authentication Authenticated user
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse containing list of API keys
     */
    @Operation(summary = "List API keys", description = "Retrieve all API keys belonging to the authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "API keys retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> getAllApiKeys(
            Authentication authentication,
            HttpServletRequest request) {
        
        telemetryClient.trackTrace("Get all API keys request received", SeverityLevel.Verbose, null);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        List<ApiKeyResponse> apiKeys = apiKeyService.getUserApiKeys(userDetails.getUser().getUserId());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "API keys retrieved successfully", apiKeys, request));
    }

    /**
     * Get specific API key details (overview)
     *
     * @param apiKeyId API key ID
     * @param authentication Authenticated user
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse containing API key details
     */
    @Operation(summary = "Get API key details", description = "Retrieve details of a specific API key by its ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "API key details retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "API key not found")
    })
    @GetMapping("/{apiKeyId}")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> getApiKeyDetails(
            @Parameter(description = "API key ID", example = "1") @PathVariable Integer apiKeyId,
            Authentication authentication,
            HttpServletRequest request) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("apiKeyId", String.valueOf(apiKeyId));
        telemetryClient.trackTrace("Get API key details request", SeverityLevel.Verbose, properties);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        ApiKeyResponse apiKeyResponse = apiKeyService.getApiKeyDetails(apiKeyId, userDetails.getUser().getUserId());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "API key details retrieved successfully", apiKeyResponse, request));
    }

    /**
     * Update API key (description and limits)
     *
     * @param apiKeyId API key ID
     * @param updateRequest Update request
     * @param authentication Authenticated user
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse containing updated API key
     */
    @Operation(summary = "Update API key", description = "Update the description and rate limits of an existing API key")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "API key updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "API key not found")
    })
    @PutMapping("/{apiKeyId}")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> updateApiKey(
            @Parameter(description = "API key ID", example = "1") @PathVariable Integer apiKeyId,
            @Valid @RequestBody UpdateApiKeyRequest updateRequest,
            Authentication authentication,
            HttpServletRequest request) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("apiKeyId", String.valueOf(apiKeyId));
        telemetryClient.trackTrace("Update API key request", SeverityLevel.Verbose, properties);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        ApiKeyResponse apiKeyResponse = apiKeyService.updateApiKey(apiKeyId, userDetails.getUser().getUserId(), updateRequest);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "API key updated successfully", apiKeyResponse, request));
    }

    /**
     * Disable/Inactive API key
     *
     * @param apiKeyId API key ID
     * @param authentication Authenticated user
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse containing updated API key
     */
    @Operation(summary = "Disable API key", description = "Disable an active API key so it can no longer be used for requests")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "API key disabled successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "API key not found")
    })
    @PatchMapping("/{apiKeyId}/disable")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> disableApiKey(
            @Parameter(description = "API key ID", example = "1") @PathVariable Integer apiKeyId,
            Authentication authentication,
            HttpServletRequest request) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("apiKeyId", String.valueOf(apiKeyId));
        telemetryClient.trackTrace("Disable API key request", SeverityLevel.Verbose, properties);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        ApiKeyResponse apiKeyResponse = apiKeyService.toggleApiKeyStatus(apiKeyId, userDetails.getUser().getUserId(), true);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "API key disabled successfully", apiKeyResponse, request));
    }

    /**
     * Enable API key
     *
     * @param apiKeyId API key ID
     * @param authentication Authenticated user
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse containing updated API key
     */
    @Operation(summary = "Enable API key", description = "Re-enable a previously disabled API key")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "API key enabled successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "API key not found")
    })
    @PatchMapping("/{apiKeyId}/enable")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> enableApiKey(
            @Parameter(description = "API key ID", example = "1") @PathVariable Integer apiKeyId,
            Authentication authentication,
            HttpServletRequest request) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("apiKeyId", String.valueOf(apiKeyId));
        telemetryClient.trackTrace("Enable API key request", SeverityLevel.Verbose, properties);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        ApiKeyResponse apiKeyResponse = apiKeyService.toggleApiKeyStatus(apiKeyId, userDetails.getUser().getUserId(), false);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "API key enabled successfully", apiKeyResponse, request));
    }

    /**
     * Delete API key
     *
     * @param apiKeyId API key ID
     * @param authentication Authenticated user
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse indicating deletion
     */
    @Operation(summary = "Delete API key", description = "Permanently delete an API key")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "API key deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "API key not found")
    })
    @DeleteMapping("/{apiKeyId}")
    public ResponseEntity<ApiResponse<Void>> deleteApiKey(
            @Parameter(description = "API key ID", example = "1") @PathVariable Integer apiKeyId,
            Authentication authentication,
            HttpServletRequest request) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("apiKeyId", String.valueOf(apiKeyId));
        telemetryClient.trackTrace("Delete API key request", SeverityLevel.Information, properties);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        apiKeyService.deleteApiKey(apiKeyId, userDetails.getUser().getUserId());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "API key deleted successfully", null, request));
    }
}

