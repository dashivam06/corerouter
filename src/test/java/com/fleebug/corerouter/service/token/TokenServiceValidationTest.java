package com.fleebug.corerouter.service.token;

import com.fleebug.corerouter.entity.token.UserToken;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.token.TokenType;
import com.fleebug.corerouter.repository.token.UserTokenRepository;
import com.fleebug.corerouter.security.jwt.JwtUtil;
import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceValidationTest {

    @Mock
    private TelemetryClient telemetryClient;

    @Mock
    private UserTokenRepository userTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private TokenService tokenService;

    private User testUser;
    private UserToken validRefreshToken;
    private UserToken expiredRefreshToken;
    private UserToken revokedRefreshToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1)
                .email("test@example.com")
                .build();

        validRefreshToken = UserToken.builder()
                .tokenId(1L)
                .user(testUser)
                .tokenValue("valid_refresh_token")
                .tokenType(TokenType.REFRESH)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        expiredRefreshToken = UserToken.builder()
                .tokenId(2L)
                .user(testUser)
                .tokenValue("expired_refresh_token")
                .tokenType(TokenType.REFRESH)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .revoked(false)
                .build();

        revokedRefreshToken = UserToken.builder()
                .tokenId(3L)
                .user(testUser)
                .tokenValue("revoked_refresh_token")
                .tokenType(TokenType.REFRESH)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(true)
                .build();
    }

    @Test
    // Tests that a valid refresh token successfully navigates validity checks
    void validateRefreshToken_ReturnsTrueForValidToken() {
        String token = "valid_refresh_token";
        when(userTokenRepository.findByTokenValue(token)).thenReturn(Optional.of(validRefreshToken));
        when(jwtUtil.validateToken(token)).thenReturn(true);

        boolean isValid = tokenService.validateRefreshToken(token);

        assertTrue(isValid);
        verify(jwtUtil).validateToken(token);
    }

    @Test
    // Tests that an expired refresh token returns false during token validation checks
    void validateRefreshToken_ReturnsFalseForExpiredToken() {
        String token = "expired_refresh_token";
        when(userTokenRepository.findByTokenValue(token)).thenReturn(Optional.of(expiredRefreshToken));

        boolean isValid = tokenService.validateRefreshToken(token);

        assertFalse(isValid);
        verify(jwtUtil, never()).validateToken(token);
    }

    @Test
    // Tests that a revoked refresh token successfully returns false instead of throwing validation errors to the client
    void validateRefreshToken_ReturnsFalseForRevokedToken() {
        String token = "revoked_refresh_token";
        when(userTokenRepository.findByTokenValue(token)).thenReturn(Optional.of(revokedRefreshToken));

        boolean isValid = tokenService.validateRefreshToken(token);

        assertFalse(isValid);
        verify(jwtUtil, never()).validateToken(token);
    }

    @Test
    // Tests extracting the user object associated with an active refresh token stored in the database
    void getUserFromRefreshToken_ReturnsUserForValidToken() {
        String token = "valid_refresh_token";
        when(userTokenRepository.findByTokenValue(token)).thenReturn(Optional.of(validRefreshToken));

        User actualUser = tokenService.getUserFromRefreshToken(token);

        assertEquals(testUser, actualUser);
        assertEquals(testUser.getEmail(), actualUser.getEmail());
    }

    @Test
    // Tests that passing an expired refresh token when attempting to extract user information throws an IllegalArgumentException
    void getUserFromRefreshToken_ThrowsExceptionForExpiredToken() {
        String token = "expired_refresh_token";
        when(userTokenRepository.findByTokenValue(token)).thenReturn(Optional.of(expiredRefreshToken));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                tokenService.getUserFromRefreshToken(token)
        );

        assertEquals("Refresh token is expired or revoked", ex.getMessage());
    }

    @Test
    // Tests that passing an invalid, missing refresh token directly throws an exception rather than returning null user entity
    void getUserFromRefreshToken_ThrowsExceptionForNonExistentToken() {
        String token = "non_existent_token";
        when(userTokenRepository.findByTokenValue(token)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                tokenService.getUserFromRefreshToken(token)
        );

        assertEquals("Invalid refresh token", ex.getMessage());
    }
}
