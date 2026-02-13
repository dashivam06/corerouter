package com.fleebug.corerouter.controller.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.user.request.LoginRequest;
import com.fleebug.corerouter.dto.user.request.RegisterRequest;
import com.fleebug.corerouter.dto.user.request.RefreshTokenRequest;
import com.fleebug.corerouter.dto.user.response.AuthResponse;
import com.fleebug.corerouter.service.token.TokenService;
import com.fleebug.corerouter.service.user.UserService;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final TokenService tokenService;

    /**
     * Register a new user
     * 
     * @param registerRequest contains username, email, profileImage, emailSubscribed, password and confirmPassword
     * @param request HttpServletRequest to extract path and method
     * @return ResponseEntity with ApiResponse containing AuthResponse
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest registerRequest, HttpServletRequest request) {
        try {
            log.info("Register endpoint called for email: {}", registerRequest.getEmail());
            AuthResponse authResponse = userService.register(registerRequest);
            
            ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.CREATED.value())
                    .success(true)
                    .message("User registered successfully")
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(authResponse)
                    .build();
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Registration error: {}", e.getMessage());
            ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .success(false)
                    .message(e.getMessage())
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Unexpected error during registration", e);
            ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .success(false)
                    .message("An unexpected error occurred")
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Login user with email and password
     * 
     * @param loginRequest contains email and password
     * @param request HttpServletRequest to extract path and method
     * @return ResponseEntity with ApiResponse containing AuthResponse
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        try {
            log.info("Login endpoint called for email: {}", loginRequest.getEmail());
            AuthResponse authResponse = userService.login(loginRequest);
            
            ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.OK.value())
                    .success(true)
                    .message("Login successful")
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(authResponse)
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Login error: {}", e.getMessage());
            ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .success(false)
                    .message(e.getMessage())
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .success(false)
                    .message("An unexpected error occurred")
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Refresh access token using refresh token
     * 
     * @param refreshTokenRequest contains refresh token
     * @param request HttpServletRequest to extract path and method
     * @return ResponseEntity with ApiResponse containing new AuthResponse with access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest, HttpServletRequest request) {
        try {
            log.info("Refresh token endpoint called");
            
            if (!tokenService.validateRefreshToken(refreshTokenRequest.getRefreshToken())) {
                log.warn("Invalid refresh token");
                ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .success(false)
                        .message("Invalid or expired refresh token")
                        .path(request.getRequestURI())
                        .method(request.getMethod())
                        .data(null)
                        .build();
                
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            var user = tokenService.getUserFromRefreshToken(refreshTokenRequest.getRefreshToken());
            String newAccessToken = tokenService.generateAccessToken(user);
            Long expiresIn = tokenService.getAccessTokenExpirationTime(newAccessToken);
            
            AuthResponse authResponse = AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshTokenRequest.getRefreshToken())
                    .expiresIn(expiresIn)
                    .build();
            
            ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.OK.value())
                    .success(true)
                    .message("Access token refreshed successfully")
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(authResponse)
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Refresh token error: {}", e.getMessage());
            ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .success(false)
                    .message(e.getMessage())
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            log.error("Unexpected error during token refresh", e);
            ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .success(false)
                    .message("An unexpected error occurred")
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Logout user by revoking refresh token
     * 
     * @param refreshTokenRequest contains refresh token to revoke
     * @param request HttpServletRequest to extract path and method
     * @return ResponseEntity with ApiResponse indicating logout success
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest, HttpServletRequest request) {
        try {
            log.info("Logout endpoint called");
            tokenService.revokeRefreshToken(refreshTokenRequest.getRefreshToken());
            
            ApiResponse<Void> response = ApiResponse.<Void>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.OK.value())
                    .success(true)
                    .message("Logged out successfully")
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Logout error: {}", e.getMessage());
            ApiResponse<Void> response = ApiResponse.<Void>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .success(false)
                    .message(e.getMessage())
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Unexpected error during logout", e);
            ApiResponse<Void> response = ApiResponse.<Void>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .success(false)
                    .message("An unexpected error occurred")
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
