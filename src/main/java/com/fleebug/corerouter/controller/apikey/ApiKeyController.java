package com.fleebug.corerouter.controller.apikey;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/apikeys")
@RequiredArgsConstructor
@Slf4j
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * Generate a new API key
     * 
     * @param createRequest API key creation request
     * @param authentication Authenticated user
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse containing created API key
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> generateApiKey(
            @Valid @RequestBody CreateApiKeyRequest createRequest,
            Authentication authentication,
            HttpServletRequest request) {
        log.info("Generate API key request received");

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        ApiKeyResponse apiKeyResponse = apiKeyService.generateApiKey(userDetails.getUser(), createRequest);

        ApiResponse<ApiKeyResponse> response = ApiResponse.<ApiKeyResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CREATED.value())
                .success(true)
                .message("API key generated successfully")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(apiKeyResponse)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all API keys for the authenticated user
     * 
     * @param authentication Authenticated user
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse containing list of API keys
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> getAllApiKeys(
            Authentication authentication,
            HttpServletRequest request) {
        log.info("Get all API keys request received");

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        List<ApiKeyResponse> apiKeys = apiKeyService.getUserApiKeys(userDetails.getUser().getUserId());

        ApiResponse<List<ApiKeyResponse>> response = ApiResponse.<List<ApiKeyResponse>>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .success(true)
                .message("API keys retrieved successfully")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(apiKeys)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get specific API key details (overview)
     * 
     * @param apiKeyId API key ID
     * @param authentication Authenticated user
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse containing API key details
     */
    @GetMapping("/{apiKeyId}")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> getApiKeyDetails(
            @PathVariable Integer apiKeyId,
            Authentication authentication,
            HttpServletRequest request) {
        log.info("Get API key details request - ID: {}", apiKeyId);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        ApiKeyResponse apiKeyResponse = apiKeyService.getApiKeyDetails(apiKeyId, userDetails.getUser().getUserId());

        ApiResponse<ApiKeyResponse> response = ApiResponse.<ApiKeyResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .success(true)
                .message("API key details retrieved successfully")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(apiKeyResponse)
                .build();

        return ResponseEntity.ok(response);
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
    @PutMapping("/{apiKeyId}")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> updateApiKey(
            @PathVariable Integer apiKeyId,
            @Valid @RequestBody UpdateApiKeyRequest updateRequest,
            Authentication authentication,
            HttpServletRequest request) {
        log.info("Update API key request - ID: {}", apiKeyId);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        ApiKeyResponse apiKeyResponse = apiKeyService.updateApiKey(apiKeyId, userDetails.getUser().getUserId(), updateRequest);

        ApiResponse<ApiKeyResponse> response = ApiResponse.<ApiKeyResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .success(true)
                .message("API key updated successfully")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(apiKeyResponse)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Disable/Inactive API key
     * 
     * @param apiKeyId API key ID
     * @param authentication Authenticated user
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse containing updated API key
     */
    @PatchMapping("/{apiKeyId}/disable")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> disableApiKey(
            @PathVariable Integer apiKeyId,
            Authentication authentication,
            HttpServletRequest request) {
        log.info("Disable API key request - ID: {}", apiKeyId);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        ApiKeyResponse apiKeyResponse = apiKeyService.toggleApiKeyStatus(apiKeyId, userDetails.getUser().getUserId(), true);

        ApiResponse<ApiKeyResponse> response = ApiResponse.<ApiKeyResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .success(true)
                .message("API key disabled successfully")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(apiKeyResponse)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Enable API key
     * 
     * @param apiKeyId API key ID
     * @param authentication Authenticated user
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse containing updated API key
     */
    @PatchMapping("/{apiKeyId}/enable")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> enableApiKey(
            @PathVariable Integer apiKeyId,
            Authentication authentication,
            HttpServletRequest request) {
        log.info("Enable API key request - ID: {}", apiKeyId);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        ApiKeyResponse apiKeyResponse = apiKeyService.toggleApiKeyStatus(apiKeyId, userDetails.getUser().getUserId(), false);

        ApiResponse<ApiKeyResponse> response = ApiResponse.<ApiKeyResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .success(true)
                .message("API key enabled successfully")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(apiKeyResponse)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Delete API key
     * 
     * @param apiKeyId API key ID
     * @param authentication Authenticated user
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse indicating deletion
     */
    @DeleteMapping("/{apiKeyId}")
    public ResponseEntity<ApiResponse<Void>> deleteApiKey(
            @PathVariable Integer apiKeyId,
            Authentication authentication,
            HttpServletRequest request) {
        log.info("Delete API key request - ID: {}", apiKeyId);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        apiKeyService.deleteApiKey(apiKeyId, userDetails.getUser().getUserId());

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .success(true)
                .message("API key deleted successfully")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }
}

