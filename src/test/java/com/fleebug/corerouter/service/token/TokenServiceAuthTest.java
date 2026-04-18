package com.fleebug.corerouter.service.token;

import com.fleebug.corerouter.dto.user.response.AuthResponse;
import com.fleebug.corerouter.entity.token.UserToken;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.user.UserRole;
import com.fleebug.corerouter.repository.token.UserTokenRepository;
import com.fleebug.corerouter.security.jwt.JwtUtil;
import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceAuthTest {

    @Mock
    private TelemetryClient telemetryClient;

    @Mock
    private UserTokenRepository userTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private TokenService tokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1)
                .email("test@example.com")
                .fullName("Test User")
                .role(UserRole.USER)
                .build();
    }

    @Test
    // Tests that auth response is correctly built containing generated access and refresh tokens
    void buildAuthResponse_ReturnsValidAuthResponse() {
        String mockAccessToken = "mock_access_token";
        String mockRefreshToken = "mock_refresh_token";
        Long mockExpiration = 3600000L;

        when(jwtUtil.generateToken(testUser.getUserId(), testUser.getEmail(), testUser.getRole().toString())).thenReturn(mockAccessToken);
        when(jwtUtil.getTokenExpirationTimeInMs(mockAccessToken)).thenReturn(mockExpiration);
        when(jwtUtil.generateRefreshToken(testUser.getUserId())).thenReturn(mockRefreshToken);
        when(jwtUtil.getTokenExpirationTimeInMs(mockRefreshToken)).thenReturn(mockExpiration);

        AuthResponse response = tokenService.buildAuthResponse(testUser);

        assertNotNull(response);
        assertEquals(mockAccessToken, response.getAccessToken());
        assertEquals(mockRefreshToken, response.getRefreshToken());
        assertEquals(mockExpiration, response.getExpiresIn());
        assertNotNull(response.getProfile());
        assertEquals(testUser.getFullName(), response.getProfile().getFullName());
        
        // Verifies to check that both access and refresh tokens were saved
        verify(userTokenRepository, times(2)).save(any(UserToken.class));
    }

    @Test
    // Tests that an access token string is successfully generated from a User entity
    void generateAccessToken_ReturnsAccessTokenString() {
        String expectedToken = "new_access_token";
        
        when(jwtUtil.generateToken(testUser.getUserId(), testUser.getEmail(), testUser.getRole().toString()))
                .thenReturn(expectedToken);

        String actualToken = tokenService.generateAccessToken(testUser);

        assertEquals(expectedToken, actualToken);
        verify(jwtUtil).generateToken(testUser.getUserId(), testUser.getEmail(), testUser.getRole().toString());
    }

    @Test
    // Tests getting expiration time extracts accurate millisecond value from a given token
    void getAccessTokenExpirationTime_ReturnsExpirationInMs() {
        String token = "sample_token";
        Long expectedExpiration = 1800000L;

        when(jwtUtil.getTokenExpirationTimeInMs(token)).thenReturn(expectedExpiration);

        Long actualExpiration = tokenService.getAccessTokenExpirationTime(token);

        assertEquals(expectedExpiration, actualExpiration);
        verify(jwtUtil).getTokenExpirationTimeInMs(token);
    }
}
