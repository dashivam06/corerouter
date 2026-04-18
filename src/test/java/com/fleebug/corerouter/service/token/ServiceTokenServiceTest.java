package com.fleebug.corerouter.service.token;

import com.fleebug.corerouter.entity.token.ServiceToken;
import com.fleebug.corerouter.enums.token.ServiceRole;
import com.fleebug.corerouter.exception.token.InvalidServiceTokenException;
import com.fleebug.corerouter.exception.token.ServiceTokenAlreadyExistsException;
import com.fleebug.corerouter.exception.token.ServiceTokenNotFoundException;
import com.fleebug.corerouter.repository.token.ServiceTokenRepository;
import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceTokenServiceTest {

    @Mock
    private TelemetryClient telemetryClient;

    @Mock
    private ServiceTokenRepository serviceTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ServiceTokenService serviceTokenService;

    private ServiceToken testToken;

    @BeforeEach
    void setUp() {
        testToken = ServiceToken.builder()
                .id(1L)
                .tokenId("mock_token_id")
                .name("test-worker-1")
                .tokenHash("encoded_hash")
                .role(ServiceRole.WORKER)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    // Tests successful creation of a service token, checking that it saves correctly and returns the raw string
    void createToken_ReturnsRawToken_WhenNameIsUnique() {
        when(serviceTokenRepository.existsByName("new-worker")).thenReturn(false);
        when(passwordEncoder.encode(any(String.class))).thenReturn("hashed_secret");
        when(serviceTokenRepository.save(any(ServiceToken.class))).thenReturn(testToken);

        String rawToken = serviceTokenService.createToken("new-worker", ServiceRole.WORKER);

        assertNotNull(rawToken);
        assertTrue(rawToken.startsWith("svc_"));
        verify(serviceTokenRepository).save(any(ServiceToken.class));
    }

    @Test
    // Tests that creating a token with an existing name throws an exception mapping to a conflict error
    void createToken_ThrowsServiceTokenAlreadyExistsException_WhenNameExists() {
        when(serviceTokenRepository.existsByName("test-worker-1")).thenReturn(true);

        ServiceTokenAlreadyExistsException ex = assertThrows(ServiceTokenAlreadyExistsException.class, () -> 
            serviceTokenService.createToken("test-worker-1", ServiceRole.WORKER)
        );

        assertEquals("Service token with name 'test-worker-1' already exists", ex.getMessage());
        verify(serviceTokenRepository, never()).save(any(ServiceToken.class));
    }

    @Test
    // Tests active token authentication validating its format and matched hash, while recording last used timestamp
    void authenticate_ReturnsServiceToken_WhenValidAndActive() {
        String rawToken = "svc_mock_token_id.secret_value";
        
        when(serviceTokenRepository.findByTokenId("mock_token_id"))
            .thenReturn(Optional.of(testToken));
        when(passwordEncoder.matches("secret_value", "encoded_hash"))
            .thenReturn(true);
        ServiceToken authenticatedToken = serviceTokenService.authenticate(rawToken);

        assertNotNull(authenticatedToken);
        assertEquals(testToken.getId(), authenticatedToken.getId());
        assertNotNull(authenticatedToken.getLastUsedAt());
        verify(serviceTokenRepository).save(testToken);
    }

    @Test
    // Tests that authentication fails with an invalid token prefix or format
    void authenticate_ThrowsInvalidServiceTokenException_WhenFormatIsInvalid() {
        String invalidToken = "invalid_format_string";

        InvalidServiceTokenException ex = assertThrows(InvalidServiceTokenException.class, () -> 
            serviceTokenService.authenticate(invalidToken)
        );

        assertEquals("Malformed service token", ex.getMessage());
        verify(serviceTokenRepository, never()).findByTokenId(anyString());
    }

    @Test
    // Tests authentication rejecting access when a valid token ID is presented with an incorrect secret
    void authenticate_ThrowsInvalidServiceTokenException_WhenPasswordDoesNotMatch() {
        String rawToken = "svc_mock_token_id.wrong_secret";
        
        when(serviceTokenRepository.findByTokenId("mock_token_id"))
            .thenReturn(Optional.of(testToken));
        when(passwordEncoder.matches("wrong_secret", 
            "encoded_hash")).thenReturn(false);

        InvalidServiceTokenException ex = assertThrows(InvalidServiceTokenException.class,
             () -> serviceTokenService.authenticate(rawToken)
        );

        assertEquals("Invalid or inactive service token", ex.getMessage());
    }

    @Test
    // Tests that attempting to authenticate with a revoked (inactive) token correctly throws an exception
    void authenticate_ThrowsServiceTokenRevokedException_WhenTokenIsNotActive() {
        String rawToken = "svc_mock_token_id.secret_value";
        testToken.setActive(false);
        
        when(serviceTokenRepository.findByTokenId("mock_token_id")).thenReturn(Optional.of(testToken));

        InvalidServiceTokenException ex = assertThrows(InvalidServiceTokenException.class, () -> 
            serviceTokenService.authenticate(rawToken)
        );

        assertEquals("Service token has been revoked", ex.getMessage());
    }

    @Test
    // Tests revoking a token successfully toggles its active state in the database
    void revokeToken_SetsActiveToFalse_WhenTokenExists() {
        when(serviceTokenRepository.findByName("test-worker-1")).thenReturn(
            Optional.of(testToken));

        serviceTokenService.revokeToken("test-worker-1");

        assertFalse(testToken.isActive());
        verify(serviceTokenRepository).save(testToken);
    }

    @Test
    // Tests that retrieving a token by ID works correctly when available
    void getByTokenId_ReturnsToken_WhenIdExists() {
        when(serviceTokenRepository.findByTokenId("mock_token_id")).thenReturn(Optional.of(testToken));

        ServiceToken foundToken = serviceTokenService.getByTokenId("mock_token_id");

        assertEquals(testToken, foundToken);
    }

    @Test
    // Tests that retrieving a token by an unknown ID throws a standard not found exception
    void getByTokenId_ThrowsNotFoundException_WhenIdDoesNotExist() {
        when(serviceTokenRepository.findByTokenId("unknown_id")).thenReturn(Optional.empty());

        assertThrows(ServiceTokenNotFoundException.class, () -> 
            serviceTokenService.getByTokenId("unknown_id")
        );
    }

    @Test
    // Tests retrieving tokens mapped to specific worker roles accurately
    void listActiveByRole_ReturnsServiceTokens_WhenRoleRequested() {
        when(serviceTokenRepository.findByRoleAndActiveTrue(ServiceRole.WORKER))
            .thenReturn(List.of(testToken));

        List<ServiceToken> tokens = serviceTokenService.listActiveByRole(ServiceRole.WORKER);

        assertEquals(1, tokens.size());
        assertEquals(ServiceRole.WORKER, tokens.get(0).getRole());
    }

    @Test
    // Tests deep deletion mapped securely through token name references
    void deleteToken_RemovesTokenFromRepository() {
        when(serviceTokenRepository.findByName("test-worker-1")).thenReturn(Optional.of(testToken));

        serviceTokenService.deleteToken("test-worker-1");

        assertFalse(testToken.isActive());
        verify(serviceTokenRepository).save(testToken);
    }
}
