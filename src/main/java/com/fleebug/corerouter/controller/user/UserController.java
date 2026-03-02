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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User registration, login, OTP verification, token refresh, and logout")
public class UserController {

    private final UserService userService;
    private final TokenService tokenService;

    @Operation(summary = "Request OTP", description = "Step 1 — Send a one-time password to the given email address")
    @PostMapping("/request-otp")
    public ResponseEntity<ApiResponse<RequestOtpResponse>> requestOtp(
            @Valid @RequestBody RequestOtpRequest requestOtpRequest,
            HttpServletRequest servletRequest) {
        log.info("STEP 1: OTP request received for email: {}", requestOtpRequest.getEmail());
        
        RequestOtpResponse otpResponse = userService.requestOtp(requestOtpRequest.getEmail());
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                "OTP sent successfully. Proceed to verify-otp with the verification ID.",
                otpResponse, servletRequest));
    }

    @Operation(summary = "Verify OTP", description = "Step 2 — Verify the OTP to proceed with registration")
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<VerifyOtpResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest verifyOtpRequest,
            HttpServletRequest servletRequest) {
        log.info("STEP 2: OTP verification request received for verificationId: {}", verifyOtpRequest.getVerificationId());
        
        var otpResponse = userService.verifyOtp(
                verifyOtpRequest.getVerificationId(),
                verifyOtpRequest.getOtp()
        );
        
        VerifyOtpResponse response = VerifyOtpResponse.builder()
                .verificationId(otpResponse.getVerificationId())
                .message(otpResponse.getMessage())
                .verified(otpResponse.isVerified())
                .profileCompletionTtlMinutes(otpResponse.getProfileCompletionTtlMinutes())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                "OTP verified successfully. Proceed to register with profile details.",
                response, servletRequest));
    }

    @Operation(summary = "Register", description = "Step 3 — Complete registration with profile details after OTP verification")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody FinalRegistrationRequest finalRegistrationRequest,
            HttpServletRequest servletRequest) {
        log.info("STEP 3: Final registration request received for verificationId: {}", finalRegistrationRequest.getVerificationId());
        
        if (!finalRegistrationRequest.getPassword().equals(finalRegistrationRequest.getConfirmPassword())) {
            log.warn("STEP 3 error: Passwords do not match");
            throw new IllegalArgumentException("Passwords do not match");
        }
        
        AuthResponse response = userService.finalRegister(
                finalRegistrationRequest.getVerificationId(),
                finalRegistrationRequest
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED,
                        "User registered successfully. You are now logged in.",
                        response, servletRequest));
    }

    @Operation(summary = "Login", description = "Authenticate with email and password to obtain access and refresh tokens")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {
        log.info("Login endpoint called for email: {}", loginRequest.getEmail());
        AuthResponse authResponse = userService.login(loginRequest);
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Login successful", authResponse, request));
    }

    @Operation(summary = "Refresh token", description = "Exchange a valid refresh token for a new access token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest,
            HttpServletRequest request) {
        log.info("Refresh token endpoint called");
        
        if (!tokenService.validateRefreshToken(refreshTokenRequest.getRefreshToken())) {
            log.warn("Invalid refresh token");
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }
        
        var user = tokenService.getUserFromRefreshToken(refreshTokenRequest.getRefreshToken());
        String newAccessToken = tokenService.generateAccessToken(user);
        Long expiresIn = tokenService.getAccessTokenExpirationTime(newAccessToken);
        
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshTokenRequest.getRefreshToken())
                .expiresIn(expiresIn)
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Access token refreshed successfully", authResponse, request));
    }

    @Operation(summary = "Logout", description = "Revoke the refresh token to end the session")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest,
            HttpServletRequest request) {
        log.info("Logout endpoint called");
        tokenService.revokeRefreshToken(refreshTokenRequest.getRefreshToken());
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Logged out successfully", null, request));
    }
}