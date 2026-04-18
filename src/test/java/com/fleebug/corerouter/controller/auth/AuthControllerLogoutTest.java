package com.fleebug.corerouter.controller.auth;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.user.request.RefreshTokenRequest;
import com.fleebug.corerouter.service.token.TokenService;
import com.fleebug.corerouter.service.user.UserService;
import com.microsoft.applicationinsights.TelemetryClient;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the AuthController's logout endpoint.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerLogoutTest {

    @Mock
    private UserService userService;

    @Mock
    private TokenService tokenService;

    @Mock
    private TelemetryClient telemetryClient;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuthController authController;

    @Test
    // Tests that sending a valid logout request delegates to TokenService and returns successful response
    void logout_whenValidRequest_revokesTokenAndReturnsSuccess() {
        RefreshTokenRequest requestBody = new RefreshTokenRequest();
        requestBody.setRefreshToken("my-refresh-token");

        // By default, mock tokenService.revokeRefreshToken does nothing and doesn't throw
        doNothing().when(tokenService).revokeRefreshToken("my-refresh-token");
        when(httpServletRequest.getRequestURI()).thenReturn("/api/v1/auth/logout");

        ResponseEntity<ApiResponse<Void>> response = authController.logout(requestBody, httpServletRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ApiResponse<Void> apiResponse = response.getBody();
        assertNotNull(apiResponse);
        assertEquals(200, apiResponse.getStatus());
        assertEquals("Logged out successfully", apiResponse.getMessage());
        assertNull(apiResponse.getData());
        assertEquals("/api/v1/auth/logout", apiResponse.getPath());

        verify(tokenService).revokeRefreshToken("my-refresh-token");
    }

    @Test
    // Tests that if token revocation throws an exception (e.g., token already revoked/invalid), the exception bubbles up
    void logout_whenTokenInvalid_throwsIllegalArgumentException() {
        RefreshTokenRequest requestBody = new RefreshTokenRequest();
        requestBody.setRefreshToken("invalid-refresh-token");

        doThrow(new IllegalArgumentException("Invalid refresh token"))
                .when(tokenService).revokeRefreshToken("invalid-refresh-token");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authController.logout(requestBody, httpServletRequest)
        );

        assertEquals("Invalid refresh token", ex.getMessage());
        verify(tokenService).revokeRefreshToken("invalid-refresh-token");
    }
}
