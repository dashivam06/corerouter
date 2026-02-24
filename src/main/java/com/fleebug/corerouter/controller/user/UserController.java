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

    @PostMapping("/request-otp")
    public ResponseEntity<ApiResponse<RequestOtpResponse>> requestOtp(
            @Valid @RequestBody RequestOtpRequest requestOtpRequest,
            HttpServletRequest servletRequest) {
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
    }

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
    }

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
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {
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
    }

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
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest,
            HttpServletRequest request) {
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
    }
}