package com.fleebug.corerouter.service.apikey;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fleebug.corerouter.dto.apikey.request.CreateApiKeyRequest;
import com.fleebug.corerouter.dto.apikey.request.UpdateApiKeyRequest;
import com.fleebug.corerouter.dto.apikey.response.ApiKeyResponse;
import com.fleebug.corerouter.entity.apikey.ApiKey;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.apikey.ApiKeyStatus;
import com.fleebug.corerouter.repository.apikey.ApiKeyRepository;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ApiKeyService {

    private final TelemetryClient telemetryClient;
    private final ApiKeyRepository apiKeyRepository;

    @Value("${security.apikey.pepper}")
    private String pepper;

    /**
     * Generate a new API key for user
     * 
     * @param user User object
     * @param createRequest Create API key request
     * @return ApiKeyResponse with generated key (RAW key only shown ONCE)
     */
    public ApiKeyResponse generateApiKey(User user, CreateApiKeyRequest createRequest) {
        // telemetryClient.trackTrace("Generating new API key for user ID: " + user.getUserId(), SeverityLevel.Verbose, Map.of("userId", String.valueOf(user.getUserId())));

        // 1. Generate the RAW key (cr_live_...)
        String rawKey = generateRawKey(user.getUserId());
        
        // 2. Hash it for storage
        String hashedKey = hashKey(rawKey);

        // 3. Ensure hash uniqueness (though collision is extremely unlikely)
        if (apiKeyRepository.existsByKey(hashedKey)) {
             throw new IllegalStateException("Key collision error. Please try again.");
        }

        ApiKey apiKey = ApiKey.builder()
                .user(user)
                .key(hashedKey) // STORE THE HASH
                .description(createRequest.getDescription())
                .dailyLimit(createRequest.getDailyLimit())
                .monthlyLimit(createRequest.getMonthlyLimit())
                .status(ApiKeyStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        ApiKey savedApiKey = apiKeyRepository.save(apiKey);
        telemetryClient.trackTrace("API key generated successfully for user ID: " + user.getUserId(), SeverityLevel.Information, Map.of("userId", String.valueOf(user.getUserId())));

        // 4. Return the RAW key to the user (Response only)
        ApiKeyResponse response = mapToResponse(savedApiKey);
        response.setKey(rawKey); // Override the hash with the raw key for display
        return response;
    }

    /**
     * Get all API keys for user
     * 
     * @param userId User ID
     * @return List of API key responses
     */
    public List<ApiKeyResponse> getUserApiKeys(Integer userId) {
        // telemetryClient.trackTrace("Fetching all API keys for user ID: " + userId, SeverityLevel.Verbose, Map.of("userId", String.valueOf(userId)));
        List<ApiKey> apiKeys = apiKeyRepository.findByUserUserId(userId);
        return apiKeys.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get specific API key details
     * 
     * @param apiKeyId API key ID
     * @param userId User ID (to verify ownership)
     * @return ApiKeyResponse
     */
    public ApiKeyResponse getApiKeyDetails(Integer apiKeyId, Integer userId) {
        // telemetryClient.trackTrace("Fetching API key details - ID: " + apiKeyId + ", User ID: " + userId, SeverityLevel.Verbose, Map.of("apiKeyId", String.valueOf(apiKeyId), "userId", String.valueOf(userId)));

        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));

        // Verify user owns this API key
        if (!apiKey.getUser().getUserId().equals(userId)) {
            telemetryClient.trackTrace("Unauthorized access attempt for API key ID: " + apiKeyId, SeverityLevel.Warning, Map.of("apiKeyId", String.valueOf(apiKeyId)));
            throw new IllegalArgumentException("You do not have permission to access this API key");
        }

        return mapToResponse(apiKey);
    }

    /**
     * Update API key (description and limits)
     * 
     * @param apiKeyId API key ID
     * @param userId User ID (to verify ownership)
     * @param updateRequest Update request
     * @return Updated ApiKeyResponse
     */
    public ApiKeyResponse updateApiKey(Integer apiKeyId, Integer userId, UpdateApiKeyRequest updateRequest) {
        // telemetryClient.trackTrace("Updating API key - ID: " + apiKeyId + ", User ID: " + userId, SeverityLevel.Verbose, Map.of("apiKeyId", String.valueOf(apiKeyId), "userId", String.valueOf(userId)));

        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));

        // Verify user owns this API key
        if (!apiKey.getUser().getUserId().equals(userId)) {
            telemetryClient.trackTrace("Unauthorized update attempt for API key ID: " + apiKeyId, SeverityLevel.Warning, Map.of("apiKeyId", String.valueOf(apiKeyId)));
            throw new IllegalArgumentException("You do not have permission to modify this API key");
        }

        // Update fields
        if (updateRequest.getDescription() != null) {
            apiKey.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getDailyLimit() != null) {
            apiKey.setDailyLimit(updateRequest.getDailyLimit());
        }
        if (updateRequest.getMonthlyLimit() != null) {
            apiKey.setMonthlyLimit(updateRequest.getMonthlyLimit());
        }

        ApiKey updatedApiKey = apiKeyRepository.save(apiKey);
        telemetryClient.trackTrace("API key updated successfully - ID: " + apiKeyId, SeverityLevel.Information, Map.of("apiKeyId", String.valueOf(apiKeyId)));

        return mapToResponse(updatedApiKey);
    }

    /**
     * Disable/disable API key
     * 
     * @param apiKeyId API key ID
     * @param userId User ID (to verify ownership)
     * @param disable true to disable, false to enable
     * @return Updated ApiKeyResponse
     */
    public ApiKeyResponse toggleApiKeyStatus(Integer apiKeyId, Integer userId, boolean disable) {
        telemetryClient.trackTrace("Toggling API key status - ID: " + apiKeyId + ", User ID: " + userId + ", Disable: " + disable, SeverityLevel.Information, Map.of("apiKeyId", String.valueOf(apiKeyId)));

        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));

        // Verify user owns this API key
        if (!apiKey.getUser().getUserId().equals(userId)) {
            telemetryClient.trackTrace("Unauthorized status toggle attempt for API key ID: " + apiKeyId, SeverityLevel.Warning, Map.of("apiKeyId", String.valueOf(apiKeyId)));
            throw new IllegalArgumentException("You do not have permission to modify this API key");
        }

        ApiKeyStatus newStatus = disable ? ApiKeyStatus.INACTIVE : ApiKeyStatus.ACTIVE;
        apiKey.setStatus(newStatus);

        ApiKey updatedApiKey = apiKeyRepository.save(apiKey);
        telemetryClient.trackTrace("API key status updated successfully - ID: " + apiKeyId + ", New Status: " + newStatus, SeverityLevel.Information, Map.of("apiKeyId", String.valueOf(apiKeyId)));

        return mapToResponse(updatedApiKey);
    }

    /**
     * Delete API key
     * 
     * @param apiKeyId API key ID
     * @param userId User ID (to verify ownership)
     */
    public void deleteApiKey(Integer apiKeyId, Integer userId) {
        telemetryClient.trackTrace("Deleting API key - ID: " + apiKeyId + ", User ID: " + userId, SeverityLevel.Information, Map.of("apiKeyId", String.valueOf(apiKeyId)));

        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));

        // Verify user owns this API key
        if (!apiKey.getUser().getUserId().equals(userId)) {
            telemetryClient.trackTrace("Unauthorized delete attempt for API key ID: " + apiKeyId, SeverityLevel.Warning, Map.of("apiKeyId", String.valueOf(apiKeyId)));
            throw new IllegalArgumentException("You do not have permission to delete this API key");
        }

        apiKeyRepository.deleteById(apiKeyId);
        telemetryClient.trackTrace("API key deleted successfully - ID: " + apiKeyId, SeverityLevel.Information, Map.of("apiKeyId", String.valueOf(apiKeyId)));
    }

    private String generateRawKey(Integer userId) {
        return "cr_live_" + userId + "_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    public String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Combine raw key with server-side pepper for additional security
            byte[] hash = digest.digest((rawKey + pepper).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing API key", e);
        }
    }


    /**
     * Map ApiKey entity to ApiKeyResponse DTO
     * 
     * @param apiKey ApiKey entity
     * @return ApiKeyResponse
     */
    private ApiKeyResponse mapToResponse(ApiKey apiKey) {
        return ApiKeyResponse.builder()
                .apiKeyId(apiKey.getApiKeyId())
                .key(apiKey.getKey()) // This will contain the HASH usually
                .description(apiKey.getDescription())
                .dailyLimit(apiKey.getDailyLimit())
                .monthlyLimit(apiKey.getMonthlyLimit())
                .status(apiKey.getStatus())
                .createdAt(apiKey.getCreatedAt())
                .lastUsedAt(apiKey.getLastUsedAt())
                .build();
    }
}
