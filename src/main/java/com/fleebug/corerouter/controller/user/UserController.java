package com.fleebug.corerouter.controller.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.otp.FinalRegistrationRequest;
import com.fleebug.corerouter.dto.otp.RequestOtpRequest;
import com.fleebug.corerouter.dto.otp.RequestOtpResponse;
import com.fleebug.corerouter.dto.otp.VerifyOtpRequest;
import com.fleebug.corerouter.dto.otp.VerifyOtpResponse;
import com.fleebug.corerouter.dto.user.request.LoginRequest;
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
     * Request OTP Process for Registration
     * 
     * User provides only email
     * Server generates verificationId and sends OTP to email
     * 
     * @param requestOtpRequest contains email only
     * @param servletRequest HttpServletRequest to extract path and method
     * @return ResponseEntity with verificationId
     */
    @PostMapping("/request-otp")
    public ResponseEntity<ApiResponse<RequestOtpResponse>> requestOtp(
            @Valid @RequestBody RequestOtpRequest requestOtpRequest,
            HttpServletRequest servletRequest) {
        try {
            log.info("STEP 1: OTP request received for email: {}", requestOtpRequest.getEmail());
            
            RequestOtpResponse otpResponse = userService.requestOtp(requestOtpRequest.getEmail());
            
            ApiResponse<RequestOtpResponse> apiResponse = ApiResponse.<RequestOtpResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.OK.value())
                    .success(true)
                    .message("OTP sent successfully. Proceed to verify-otp with the verification ID.")
                    .path(servletRequest.getRequestURI())
                    .method(servletRequest.getMethod())
                    .data(otpResponse)
                    .build();
            
            return ResponseEntity.ok(apiResponse);
        } catch (IllegalArgumentException e) {
            log.error("STEP 1 error: {}", e.getMessage());
            ApiResponse<RequestOtpResponse> apiResponse = ApiResponse.<RequestOtpResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .success(false)
                    .message(e.getMessage())
                    .path(servletRequest.getRequestURI())
                    .method(servletRequest.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error during STEP 1", e);
            ApiResponse<RequestOtpResponse> apiResponse = ApiResponse.<RequestOtpResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .success(false)
                    .message("An unexpected error occurred")
                    .path(servletRequest.getRequestURI())
                    .method(servletRequest.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    /**
     * Verify OTP Process for Registration
     * 
     * User provides verificationId (from step 1) + OTP code from email
     * If valid, verificationId becomes proof token for step 3
     * Does NOT create user account yet - only validates OTP
     * 
     * @param verifyOtpRequest contains verificationId and otp
     * @param servletRequest HttpServletRequest to extract path and method
     * @return ResponseEntity with VerifyOtpResponse
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<VerifyOtpResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest verifyOtpRequest,
            HttpServletRequest servletRequest) {
        try {
            log.info("STEP 2: OTP verification request received for verificationId: {}", verifyOtpRequest.getVerificationId());
            
            var otpResponse = userService.verifyOtp(
                    verifyOtpRequest.getVerificationId(),
                    verifyOtpRequest.getOtp()
            );
            
            // Transform OtpResponseDto to VerifyOtpResponse
            VerifyOtpResponse response = VerifyOtpResponse.builder()
                    .verificationId(otpResponse.getVerificationId())
                    .message(otpResponse.getMessage())
                    .verified(otpResponse.isVerified())
                    .profileCompletionTtlMinutes(otpResponse.getProfileCompletionTtlMinutes())
                    .build();
            
            ApiResponse<VerifyOtpResponse> apiResponse = ApiResponse.<VerifyOtpResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.OK.value())
                    .success(true)
                    .message("OTP verified successfully. Proceed to register with profile details.")
                    .path(servletRequest.getRequestURI())
                    .method(servletRequest.getMethod())
                    .data(response)
                    .build();
            
            return ResponseEntity.ok(apiResponse);
        } catch (IllegalArgumentException e) {
            log.error("STEP 2 error: {}", e.getMessage());
            ApiResponse<VerifyOtpResponse> apiResponse = ApiResponse.<VerifyOtpResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .success(false)
                    .message(e.getMessage())
                    .path(servletRequest.getRequestURI())
                    .method(servletRequest.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error during STEP 2", e);
            ApiResponse<VerifyOtpResponse> apiResponse = ApiResponse.<VerifyOtpResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .success(false)
                    .message("An unexpected error occurred")
                    .path(servletRequest.getRequestURI())
                    .method(servletRequest.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    /**
     * User Registration Process Finally
     * 
     * User provides verificationId (proof from step 2) + profile details
     * Creates user account with fullName, password, and profileImage
     * Returns authentication tokens (access + refresh)
     * 
     * @param finalRegistrationRequest contains verificationId, fullName, password, confirmPassword, profileImage, emailSubscribed
     * @param servletRequest HttpServletRequest to extract path and method
     * @return ResponseEntity with AuthResponse (tokenized)
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody FinalRegistrationRequest finalRegistrationRequest,
            HttpServletRequest servletRequest) {
        try {
            log.info("STEP 3: Final registration request received for verificationId: {}", finalRegistrationRequest.getVerificationId());
            
            // Validate that passwords match
            if (!finalRegistrationRequest.getPassword().equals(finalRegistrationRequest.getConfirmPassword())) {
                log.warn("STEP 3 error: Passwords do not match");
                ApiResponse<AuthResponse> apiResponse = ApiResponse.<AuthResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .success(false)
                        .message("Passwords do not match")
                        .path(servletRequest.getRequestURI())
                        .method(servletRequest.getMethod())
                        .data(null)
                        .build();
                
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
            }
            
            AuthResponse response = userService.finalRegister(
                    finalRegistrationRequest.getVerificationId(),
                    finalRegistrationRequest
            );
            
            ApiResponse<AuthResponse> apiResponse = ApiResponse.<AuthResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.CREATED.value())
                    .success(true)
                    .message("User registered successfully. You are now logged in.")
                    .path(servletRequest.getRequestURI())
                    .method(servletRequest.getMethod())
                    .data(response)
                    .build();
            
            return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
        } catch (IllegalArgumentException e) {
            log.error("STEP 3 error: {}", e.getMessage());
            ApiResponse<AuthResponse> apiResponse = ApiResponse.<AuthResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .success(false)
                    .message(e.getMessage())
                    .path(servletRequest.getRequestURI())
                    .method(servletRequest.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error during STEP 3", e);
            ApiResponse<AuthResponse> apiResponse = ApiResponse.<AuthResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .success(false)
                    .message("An unexpected error occurred")
                    .path(servletRequest.getRequestURI())
                    .method(servletRequest.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
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
