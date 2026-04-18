package com.fleebug.corerouter.service.apikey;

import com.fleebug.corerouter.dto.apikey.request.CreateApiKeyRequest;
import com.fleebug.corerouter.dto.apikey.request.UpdateApiKeyRequest;
import com.fleebug.corerouter.dto.apikey.response.ApiKeyResponse;
import com.fleebug.corerouter.entity.apikey.ApiKey;
import com.fleebug.corerouter.entity.apikey.ApiKeyStatusAudit;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.apikey.ApiKeyStatus;
import com.fleebug.corerouter.enums.user.UserRole;
import com.fleebug.corerouter.enums.user.UserStatus;
import com.fleebug.corerouter.exception.apikey.ApiKeyNotFoundException;
import com.fleebug.corerouter.exception.apikey.ApiKeyRevokedException;
import com.fleebug.corerouter.repository.apikey.ApiKeyRepository;
import com.fleebug.corerouter.repository.apikey.ApiKeyStatusAuditRepository;
import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceUserActivityTest {

    @Mock
    private TelemetryClient telemetryClient;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private ApiKeyStatusAuditRepository apiKeyStatusAuditRepository;

    @InjectMocks
    private ApiKeyService apiKeyService;

    private User testUser;
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(apiKeyService, "pepper", "test-pepper-value");

        testUser = User.builder()
                .userId(1)
                .email("test@example.com")
                .fullName("Test User")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build();
    }

    // --- generateApiKey ---

    @Test
    // Tests that a new API key is generated successfully, the raw key is returned, and the key is hashed in the db
    void generateApiKey_whenSuccess_returnsResponseWithRawKey() {
        CreateApiKeyRequest request = new CreateApiKeyRequest();
        request.setDescription("My new key");
        request.setDailyLimit(100);
        request.setMonthlyLimit(1000);

        when(apiKeyRepository.existsByKey(anyString())).thenReturn(false);

        // Since the key is generated dynamically, we capture the entity to return it in the save mock
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> {
            ApiKey saved = invocation.getArgument(0);
            saved.setApiKeyId(10);
            return saved;
        });

        ApiKeyResponse response = apiKeyService.generateApiKey(testUser, request);

        assertNotNull(response);
        assertEquals(10, response.getApiKeyId());
        // The service should return the RAW key for the user to see, not the hashed one
        assertTrue(response.getKey().startsWith("cr_live_1_"));
        assertEquals("My new key", response.getDescription());
        assertEquals(100, response.getDailyLimit());
        assertEquals(1000, response.getMonthlyLimit());
        assertEquals(ApiKeyStatus.ACTIVE, response.getStatus());

        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).save(captor.capture());
        ApiKey savedEntity = captor.getValue();

        // Ensure we saved the hash, not the raw key
        assertNotEquals(response.getKey(), savedEntity.getKey());
    }



    // --- getUserApiKeys ---

    @Test
    // Tests that all non-revoked API keys belonging to a user are retrieved correctly
    void getUserApiKeys_whenCalled_returnsNonRevokedKeys() {
        ApiKey key1 = createTestApiKey(1, ApiKeyStatus.ACTIVE);
        ApiKey key2 = createTestApiKey(2, ApiKeyStatus.INACTIVE);

        when(apiKeyRepository.findByUserUserIdAndStatusNot(testUser.getUserId(), ApiKeyStatus.REVOKED))
                .thenReturn(List.of(key1, key2));

        List<ApiKeyResponse> responses = apiKeyService.getUserApiKeys(testUser.getUserId());

        assertEquals(2, responses.size());
        assertEquals(1, responses.get(0).getApiKeyId());
        assertEquals(ApiKeyStatus.ACTIVE, responses.get(0).getStatus());
        assertEquals(2, responses.get(1).getApiKeyId());
        assertEquals(ApiKeyStatus.INACTIVE, responses.get(1).getStatus());
    }

    // --- getApiKeyDetails ---

    @Test
    // Tests that details of a valid, owned API key are successfully fetched
    void getApiKeyDetails_whenOwnedAndValid_returnsDetails() {
        ApiKey key = createTestApiKey(1, ApiKeyStatus.ACTIVE);
        when(apiKeyRepository.findById(1)).thenReturn(Optional.of(key));

        ApiKeyResponse response = apiKeyService.getApiKeyDetails(1, testUser.getUserId());

        assertNotNull(response);
        assertEquals(1, response.getApiKeyId());
        assertEquals(ApiKeyStatus.ACTIVE, response.getStatus());
    }

    @Test
    // Tests that requesting details for a non-existent API key throws an exception
    void getApiKeyDetails_whenNotFound_throwsApiKeyNotFoundException() {
        when(apiKeyRepository.findById(999)).thenReturn(Optional.empty());

        ApiKeyNotFoundException ex = assertThrows(ApiKeyNotFoundException.class, () -> 
            apiKeyService.getApiKeyDetails(999, testUser.getUserId())
        );

        assertEquals("API key with ID '999' not found", ex.getMessage());
    }

    @Test
    // Tests that requesting a key owned by another user throws a permission exception
    void getApiKeyDetails_whenNotOwned_throwsIllegalArgumentException() {
        ApiKey key = createTestApiKey(1, ApiKeyStatus.ACTIVE);
        User differentUser = User.builder().userId(99).build();
        key.setUser(differentUser); // belongs to someone else

        when(apiKeyRepository.findById(1)).thenReturn(Optional.of(key));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> 
            apiKeyService.getApiKeyDetails(1, testUser.getUserId())
        );

        assertEquals("You do not have permission to access this API key", ex.getMessage());
    }

    @Test
    // Tests that requesting details for a revoked API key throws a not found exception
    void getApiKeyDetails_whenRevoked_throwsApiKeyRevokedException() {
        ApiKey key = createTestApiKey(1, ApiKeyStatus.REVOKED);
        when(apiKeyRepository.findById(1)).thenReturn(Optional.of(key));

        ApiKeyRevokedException ex = assertThrows(ApiKeyRevokedException.class, () -> 
            apiKeyService.getApiKeyDetails(1, testUser.getUserId())
        );

        assertEquals("API key has been revoked and cannot be used", ex.getMessage());
    }

    // --- updateApiKey ---

    @Test
    // Tests that updating an API key only modifies explicitly provided, non-null fields
    void updateApiKey_whenValid_updatesOnlyNotNullFields() {
        ApiKey key = createTestApiKey(1, ApiKeyStatus.ACTIVE);
        key.setDescription("Old Desc");
        key.setDailyLimit(100);
        key.setMonthlyLimit(1000);

        when(apiKeyRepository.findById(1)).thenReturn(Optional.of(key));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateApiKeyRequest request = new UpdateApiKeyRequest();
        request.setDescription("New Desc");
        request.setMonthlyLimit(5000);
        // dailyLimit remains null

        ApiKeyResponse response = apiKeyService.updateApiKey(1, testUser.getUserId(), request);

        assertEquals("New Desc", response.getDescription());
        assertEquals(100, response.getDailyLimit()); // Unchanged
        assertEquals(5000, response.getMonthlyLimit());
    }

    // --- toggleApiKeyStatus ---

    @Test
    // Tests that disabling an API key sets status to INACTIVE and creates an audit record
    void toggleApiKeyStatus_whenDisabling_changesStatusToInactiveAndCreatesAudit() {
        ApiKey key = createTestApiKey(1, ApiKeyStatus.ACTIVE);

        when(apiKeyRepository.findById(1)).thenReturn(Optional.of(key));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        ApiKeyResponse response = apiKeyService.toggleApiKeyStatus(1, testUser.getUserId(), true);

        assertEquals(ApiKeyStatus.INACTIVE, response.getStatus());

        ArgumentCaptor<ApiKeyStatusAudit> auditCaptor = ArgumentCaptor.forClass(ApiKeyStatusAudit.class);
        verify(apiKeyStatusAuditRepository).save(auditCaptor.capture());

        ApiKeyStatusAudit audit = auditCaptor.getValue();
        assertEquals(ApiKeyStatus.ACTIVE, audit.getOldStatus());
        assertEquals(ApiKeyStatus.INACTIVE, audit.getNewStatus());
        assertEquals("user:" + testUser.getUserId(), audit.getChangedBy());
    }

    @Test
    // Tests that enabling an INACTIVE API key status to ACTIVE and it creates an audit record
    void toggleApiKeyStatus_whenEnabling_changesStatusToActiveAndCreatesAudit() {
        ApiKey key = createTestApiKey(1, ApiKeyStatus.INACTIVE);

        when(apiKeyRepository.findById(1)).thenReturn(Optional.of(key));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        ApiKeyResponse response = apiKeyService.toggleApiKeyStatus(1, testUser.getUserId(),
         false);

        assertEquals(ApiKeyStatus.ACTIVE, response.getStatus());

        ArgumentCaptor<ApiKeyStatusAudit> auditCaptor = ArgumentCaptor.forClass(
            ApiKeyStatusAudit.class);
        verify(apiKeyStatusAuditRepository).save(auditCaptor.capture());

        ApiKeyStatusAudit audit = auditCaptor.getValue();
        assertEquals(ApiKeyStatus.INACTIVE, audit.getOldStatus());
        assertEquals(ApiKeyStatus.ACTIVE, audit.getNewStatus());
    }

    

    @Test
    // Tests that deleting an API key successfully sets status to REVOKED and logs the action
    void deleteApiKey_whenValid_softDeletesKeyAndCreatesAudit() {
        ApiKey key = createTestApiKey(1, ApiKeyStatus.ACTIVE);

        when(apiKeyRepository.findById(1)).thenReturn(Optional.of(key));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        apiKeyService.deleteApiKey(1, testUser.getUserId());

        assertEquals(ApiKeyStatus.REVOKED, key.getStatus());

        ArgumentCaptor<ApiKeyStatusAudit> auditCaptor = ArgumentCaptor.forClass(ApiKeyStatusAudit.class);
        verify(apiKeyStatusAuditRepository).save(auditCaptor.capture());

        ApiKeyStatusAudit audit = auditCaptor.getValue();
        assertEquals(ApiKeyStatus.ACTIVE, audit.getOldStatus());
        assertEquals(ApiKeyStatus.REVOKED, audit.getNewStatus());
        assertEquals("User revoked API key", audit.getReason());
    }

    // --- Utility ---

    private ApiKey createTestApiKey(Integer id, ApiKeyStatus status) {
        return ApiKey.builder()
                .apiKeyId(id)
                .user(testUser)
                .key("hashed_key_" + id)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
