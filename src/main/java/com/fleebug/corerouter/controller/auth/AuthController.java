package com.fleebug.corerouter.controller.auth;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
@Tag(name = "Authentication", description = "User registration, login, OTP verification, token refresh, and logout")
public class AuthController {

    private final UserService userService;
    private final TokenService tokenService;
    private final TelemetryClient telemetryClient;

    @Operation(summary = "Request OTP", description = "Step 1 — Send a one-time password to the given email address")
    @PostMapping("/request-otp")
    public ResponseEntity<ApiResponse<RequestOtpResponse>> requestOtp(
            @Valid @RequestBody RequestOtpRequest requestOtpRequest,
            HttpServletRequest servletRequest) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("email", requestOtpRequest.getEmail());
        telemetryClient.trackEvent("OTP_REQUEST", properties, null);
        
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
        
        Map<String, String> properties = new HashMap<>();
        properties.put("verificationId", verifyOtpRequest.getVerificationId());
        telemetryClient.trackTrace("OTP verification request", SeverityLevel.Verbose, properties);
        
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
        
        Map<String, String> properties = new HashMap<>();
        properties.put("verificationId", finalRegistrationRequest.getVerificationId());
        telemetryClient.trackTrace("User registration request", SeverityLevel.Verbose, properties);
        
        if (!finalRegistrationRequest.getPassword().equals(finalRegistrationRequest.getConfirmPassword())) {
            telemetryClient.trackTrace("Registration failed: Passwords do not match", SeverityLevel.Warning, properties);
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
        
        Map<String, String> properties = new HashMap<>();
        properties.put("email", loginRequest.getEmail());
        telemetryClient.trackTrace("Login endpoint called", SeverityLevel.Verbose, properties);
        
        AuthResponse authResponse = userService.login(loginRequest);
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Login successful", authResponse, request));
    }

    @Operation(summary = "Refresh token", description = "Exchange a valid refresh token for a new access token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest,
            HttpServletRequest request) {
        
        telemetryClient.trackTrace("Refresh token request", SeverityLevel.Verbose, null);
        
        if (!tokenService.validateRefreshToken(refreshTokenRequest.getRefreshToken())) {
            telemetryClient.trackTrace("Refresh token failed: invalid or expired", SeverityLevel.Verbose, null);
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
        
        telemetryClient.trackTrace("Logout endpoint called", SeverityLevel.Verbose, null);
        
        tokenService.revokeRefreshToken(refreshTokenRequest.getRefreshToken());
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Logged out successfully", null, request));
    }
}