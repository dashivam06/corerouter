package com.fleebug.corerouter.service.token;

import com.fleebug.corerouter.entity.token.UserToken;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.repository.token.UserTokenRepository;
import com.fleebug.corerouter.security.jwt.JwtUtil;
import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for logout and token revocation activities.
 */
@ExtendWith(MockitoExtension.class)
class TokenServiceLogoutTest {

    @Mock
    private TelemetryClient telemetryClient;

    @Mock
    private UserTokenRepository userTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private TokenService tokenService;

    private User testUser;
    private UserToken mockUserToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1)
                .email("test@example.com")
                .build();

        mockUserToken = UserToken.builder()
                .tokenId(100L)
                .user(testUser)
                .tokenValue("valid_refresh_token")
                .revoked(false)
                .build();
    }

    @Test
    // Tests that attempting to revoke a valid refresh token sets its flag to revoked and saves it to the database
    void revokeRefreshToken_whenValidToken_setsRevokedAndSaves() {
        when(userTokenRepository.findByTokenValue("valid_refresh_token")).thenReturn(Optional.of(mockUserToken));

        tokenService.revokeRefreshToken("valid_refresh_token");

        assertTrue(mockUserToken.isRevoked(), "The user token's revoked flag should be true after logout");
        verify(userTokenRepository).save(mockUserToken);
    }

    @Test
    // Tests that passing an invalid or non-existent refresh token throws an IllegalArgumentException
    void revokeRefreshToken_whenTokenInvalid_throwsIllegalArgumentException() {
        when(userTokenRepository.findByTokenValue("invalid_refresh_token")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                tokenService.revokeRefreshToken("invalid_refresh_token")
        );

        assertEquals("Invalid refresh token", ex.getMessage());
        verify(userTokenRepository, never()).save(any(UserToken.class));
    }
}
