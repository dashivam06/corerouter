package com.fleebug.corerouter.service.apikey;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fleebug.corerouter.dto.apikey.request.CreateApiKeyRequest;
import com.fleebug.corerouter.dto.apikey.request.UpdateApiKeyRequest;
import com.fleebug.corerouter.dto.apikey.response.ApiKeyAnalyticsResponse;
import com.fleebug.corerouter.dto.apikey.response.AdminApiKeyInsightsResponse;
import com.fleebug.corerouter.dto.apikey.response.ApiKeyResponse;
import com.fleebug.corerouter.dto.apikey.response.DailyApiKeyAnalyticsResponse;
import com.fleebug.corerouter.dto.apikey.response.PaginatedApiKeyListResponse;
import com.fleebug.corerouter.entity.apikey.ApiKey;
import com.fleebug.corerouter.entity.apikey.ApiKeyStatusAudit;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.apikey.ApiKeyStatus;
import com.fleebug.corerouter.exception.apikey.ApiKeyNotFoundException;
import com.fleebug.corerouter.exception.apikey.ApiKeyRevokedException;
import com.fleebug.corerouter.repository.apikey.ApiKeyRepository;
import com.fleebug.corerouter.repository.apikey.ApiKeyStatusAuditRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class ApiKeyService {

    private final TelemetryClient telemetryClient;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyStatusAuditRepository apiKeyStatusAuditRepository;

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
        List<ApiKey> apiKeys = apiKeyRepository.findByUserUserIdAndStatusNot(userId, ApiKeyStatus.REVOKED);
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

        ApiKey apiKey = getOwnedNonRevokedApiKey(apiKeyId, userId, "access");

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

        ApiKey apiKey = getOwnedNonRevokedApiKey(apiKeyId, userId, "modify");

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

        ApiKey apiKey = getOwnedNonRevokedApiKey(apiKeyId, userId, "modify");
        ApiKeyStatus oldStatus = apiKey.getStatus();

        ApiKeyStatus newStatus = disable ? ApiKeyStatus.INACTIVE : ApiKeyStatus.ACTIVE;
        apiKey.setStatus(newStatus);

        ApiKey updatedApiKey = apiKeyRepository.save(apiKey);
        if (oldStatus != newStatus) {
            createStatusAudit(updatedApiKey, oldStatus, newStatus, "User toggled API key status", "user:" + userId);
        }
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
        telemetryClient.trackTrace("Soft deleting API key - ID: " + apiKeyId + ", User ID: " + userId, SeverityLevel.Information, Map.of("apiKeyId", String.valueOf(apiKeyId)));

        ApiKey apiKey = getOwnedNonRevokedApiKey(apiKeyId, userId, "delete");
        ApiKeyStatus oldStatus = apiKey.getStatus();
        apiKey.setStatus(ApiKeyStatus.REVOKED);
        ApiKey savedApiKey = apiKeyRepository.save(apiKey);
        createStatusAudit(savedApiKey, oldStatus, ApiKeyStatus.REVOKED, "User revoked API key", "user:" + userId);

        telemetryClient.trackTrace("API key soft deleted successfully - ID: " + apiKeyId, SeverityLevel.Information, Map.of("apiKeyId", String.valueOf(apiKeyId)));
    }

    @Transactional(readOnly = true)
    public AdminApiKeyInsightsResponse getAdminInsights() {
        long totalKeys = apiKeyRepository.count();
        long active = apiKeyRepository.countByStatus(ApiKeyStatus.ACTIVE);
        long inactive = apiKeyRepository.countByStatus(ApiKeyStatus.INACTIVE);
        long revoked = apiKeyRepository.countByStatus(ApiKeyStatus.REVOKED);

        return AdminApiKeyInsightsResponse.builder()
                .totalKeys(totalKeys)
                .active(active)
                .inactive(inactive)
                .revoked(revoked)
                .build();
    }

    public ApiKeyResponse updateApiKeyStatusByAdmin(Integer apiKeyId, ApiKeyStatus newStatus) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new ApiKeyNotFoundException(apiKeyId));

        ApiKeyStatus oldStatus = apiKey.getStatus();
        if (oldStatus == newStatus) {
            throw new IllegalArgumentException("API key already has status " + newStatus);
        }

        apiKey.setStatus(newStatus);
        ApiKey saved = apiKeyRepository.save(apiKey);

        createStatusAudit(saved, oldStatus, newStatus, "Admin updated API key status", "admin");

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ApiKeyAnalyticsResponse getApiKeyAnalyticsByDateRange(LocalDateTime fromDate, LocalDateTime toDate) {
        telemetryClient.trackTrace("Fetching API key analytics", SeverityLevel.Information,
                Map.of("fromDate", fromDate.toString(), "toDate", toDate.toString()));

        long totalCreated = apiKeyRepository.countByCreatedAtBetween(fromDate, toDate);
        long totalRevoked = apiKeyStatusAuditRepository.countByNewStatusAndChangedAtBetween(ApiKeyStatus.REVOKED, fromDate, toDate);

        List<Object[]> createdPerDayRaw = apiKeyRepository.countCreatedPerDayBetween(fromDate, toDate);
        List<Object[]> revokedPerDayRaw = apiKeyStatusAuditRepository.countByNewStatusPerDayBetween(ApiKeyStatus.REVOKED, fromDate, toDate);

        Map<LocalDate, Long> createdPerDay = new HashMap<>();
        for (Object[] row : createdPerDayRaw) {
            LocalDate date = toLocalDate(row[0]);
            long count = ((Number) row[1]).longValue();
            createdPerDay.put(date, count);
        }

        Map<LocalDate, Long> revokedPerDay = new HashMap<>();
        for (Object[] row : revokedPerDayRaw) {
            LocalDate date = toLocalDate(row[0]);
            long count = ((Number) row[1]).longValue();
            revokedPerDay.put(date, count);
        }

        LocalDate startDate = fromDate.toLocalDate();
        LocalDate endDate = toDate.toLocalDate();
        List<DailyApiKeyAnalyticsResponse> dailyAnalytics = new java.util.ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            long created = createdPerDay.getOrDefault(date, 0L);
            long revoked = revokedPerDay.getOrDefault(date, 0L);

            if (created > 0 || revoked > 0) {
                dailyAnalytics.add(DailyApiKeyAnalyticsResponse.builder()
                        .date(date)
                        .created(created)
                        .revoked(revoked)
                        .build());
            }
        }

        return ApiKeyAnalyticsResponse.builder()
                .dailyAnalytics(dailyAnalytics)
                .totalCreated(totalCreated)
                .totalRevoked(totalRevoked)
                .build();
    }

    @Transactional(readOnly = true)
    public PaginatedApiKeyListResponse getApiKeysWithFilters(int page, int size, ApiKeyStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ApiKey> apiKeyPage = (status == null)
                ? apiKeyRepository.findAll(pageable)
                : apiKeyRepository.findByStatus(status, pageable);

        Page<ApiKeyResponse> responsePage = apiKeyPage.map(this::mapToResponse);
        return PaginatedApiKeyListResponse.fromPage(responsePage);
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private void createStatusAudit(ApiKey apiKey, ApiKeyStatus oldStatus, ApiKeyStatus newStatus, String reason, String changedBy) {
        ApiKeyStatusAudit audit = ApiKeyStatusAudit.builder()
                .apiKey(apiKey)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .reason(reason)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .build();

        apiKeyStatusAuditRepository.save(audit);
    }

    private ApiKey getOwnedNonRevokedApiKey(Integer apiKeyId, Integer userId, String operation) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new ApiKeyNotFoundException(apiKeyId));

        if (!apiKey.getUser().getUserId().equals(userId)) {
            telemetryClient.trackTrace("Unauthorized " + operation + " attempt for API key ID: " + apiKeyId, SeverityLevel.Warning, Map.of("apiKeyId", String.valueOf(apiKeyId)));
            throw new IllegalArgumentException("You do not have permission to " + operation + " this API key");
        }

        if (apiKey.getStatus() == ApiKeyStatus.REVOKED) {
            throw new ApiKeyRevokedException();
        }

        return apiKey;
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
                .userEmail(apiKey.getUser() != null ? apiKey.getUser().getEmail() : null)
                .userName(apiKey.getUser() != null ? apiKey.getUser().getFullName() : null)
                .build();
    }
}
