package com.fleebug.corerouter.service.apikey;

import com.fleebug.corerouter.dto.apikey.response.ApiKeyResponse;
import com.fleebug.corerouter.dto.apikey.response.PaginatedApiKeyListResponse;
import com.fleebug.corerouter.entity.apikey.ApiKey;
import com.fleebug.corerouter.entity.apikey.ApiKeyStatusAudit;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.apikey.ApiKeyStatus;
import com.fleebug.corerouter.exception.apikey.ApiKeyNotFoundException;
import com.fleebug.corerouter.repository.apikey.ApiKeyRepository;
import com.fleebug.corerouter.repository.apikey.ApiKeyStatusAuditRepository;
import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceAdminTest {

    @Mock private TelemetryClient telemetryClient;
    @Mock private ApiKeyRepository apiKeyRepository;
    @Mock private ApiKeyStatusAuditRepository apiKeyStatusAuditRepository;

    @InjectMocks
    private ApiKeyService apiKeyService;

    private ApiKey testApiKey;

    @BeforeEach
    void setUp() {
        User testUser = User.builder().userId(1).build();
        testApiKey = ApiKey.builder()
                .apiKeyId(10)
                .user(testUser)
                .status(ApiKeyStatus.ACTIVE)
                .build();
    }

    @Test
    // Tests that an admin can successfully change the status of an API Key
    void updateApiKeyStatusByAdmin_whenValid_updatesStatusSuccessfully() {
        when(apiKeyRepository.findById(10)).thenReturn(Optional.of(testApiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        ApiKeyResponse response = apiKeyService.updateApiKeyStatusByAdmin(10, ApiKeyStatus.REVOKED);

        assertNotNull(response);
        assertEquals(10, response.getApiKeyId());
        assertEquals(ApiKeyStatus.REVOKED, testApiKey.getStatus());
        verify(apiKeyRepository).save(testApiKey);
        verify(apiKeyStatusAuditRepository).save(any(ApiKeyStatusAudit.class));
    }

    @Test
    // Tests changing an API key to its existing status throws an exception
    void updateApiKeyStatusByAdmin_whenSameStatus_throwsIllegalArgumentException() {
        when(apiKeyRepository.findById(10)).thenReturn(Optional.of(testApiKey));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            apiKeyService.updateApiKeyStatusByAdmin(10, ApiKeyStatus.ACTIVE)
        );
        assertEquals("API key already has status ACTIVE", ex.getMessage());
        verify(apiKeyRepository, never()).save(any());
        verify(apiKeyStatusAuditRepository, never()).save(any());
    }

    @Test
    // Tests that requesting to update an invalid API key throws Not Found
    void updateApiKeyStatusByAdmin_whenNotFound_throwsApiKeyNotFoundException() {
        when(apiKeyRepository.findById(999)).thenReturn(Optional.empty());

        ApiKeyNotFoundException ex = assertThrows(ApiKeyNotFoundException.class, () ->
            apiKeyService.updateApiKeyStatusByAdmin(999, ApiKeyStatus.INACTIVE)
        );
        assertEquals("API key with ID '999' not found", ex.getMessage());
        verify(apiKeyRepository, never()).save(any());
    }

    @Test
    // Tests retrieving paginated API keys filtered safely by a particular status
    void getApiKeysWithFilters_whenFilteredByStatus_returnsPaginatedResponse() {
        Page<ApiKey> pageResult = new PageImpl<>(List.of(testApiKey), PageRequest.of(0, 10), 1);

        when(apiKeyRepository.findByStatus(eq(ApiKeyStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(pageResult);

        PaginatedApiKeyListResponse response = apiKeyService.getApiKeysWithFilters(0, 10, ApiKeyStatus.ACTIVE);

        assertNotNull(response);
        assertEquals(1, response.getApiKeys().size());
        assertEquals(10, response.getApiKeys().get(0).getApiKeyId());
        assertEquals(1, response.getTotalElements());
    }

    @Test
    // Tests retrieving paginated API keys without filters applies to all configurations natively
    void getApiKeysWithFilters_whenNoFilter_returnsAllPaginatedResponse() {
        Page<ApiKey> pageResult = new PageImpl<>(List.of(testApiKey), PageRequest.of(0, 10), 1);

        when(apiKeyRepository.findAll(any(Pageable.class))).thenReturn(pageResult);

        PaginatedApiKeyListResponse response = apiKeyService.getApiKeysWithFilters(0, 10, null);

        assertNotNull(response);
        assertEquals(1, response.getApiKeys().size());
        assertEquals(1, response.getTotalElements());
    }
}
