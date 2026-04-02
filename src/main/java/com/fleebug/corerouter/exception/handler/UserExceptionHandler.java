package com.fleebug.corerouter.exception.handler;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import lombok.RequiredArgsConstructor;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.exception.apikey.RateLimitExceededException;
import com.fleebug.corerouter.exception.user.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * User domain exception handler
 * 
 * Handles all user-related exceptions and returns standardized error responses
 * with proper HTTP status codes and logging via Azure Telemetry
 */
@RestControllerAdvice(basePackages = {
        "com.fleebug.corerouter.controller.user",
        "com.fleebug.corerouter.controller.auth"
})
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserExceptionHandler {

    private final TelemetryClient telemetryClient;
    
    /**
     * Handle UserNotFoundException
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFoundException(
            UserNotFoundException ex,
            HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("User not found", SeverityLevel.Information, properties);
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage(), request));
    }
    
    /**
     * Handle UserAlreadyExistsException
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAlreadyExistsException(
            UserAlreadyExistsException ex,
            HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("User already exists", SeverityLevel.Information, properties);
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(HttpStatus.CONFLICT, ex.getMessage(), request));
    }
    
    /**
     * Handle InvalidCredentialsException
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentialsException(
            InvalidCredentialsException ex,
            HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("Invalid credentials provided", SeverityLevel.Information, properties);
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, ex.getMessage(), request));
    }
    
    /**
     * Handle OtpExpiredException
     */
    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleOtpExpiredException(
            OtpExpiredException ex,
            HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("OTP expired", SeverityLevel.Information, properties);
        
        return ResponseEntity.status(HttpStatus.GONE)
                .body(ApiResponse.error(HttpStatus.GONE, ex.getMessage(), request));
    }
    
    /**
     * Handle InvalidOtpException
     */
    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOtpException(
            InvalidOtpException ex,
            HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("Invalid OTP provided", SeverityLevel.Information, properties);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }
    
    /**
     * Handle InvalidTokenException
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTokenException(
            InvalidTokenException ex,
            HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("Invalid token", SeverityLevel.Information, properties);
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, ex.getMessage(), request));
    }
    
    /**
     * Handle RateLimitExceededException (OTP rate limiting)
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitExceededException(
            RateLimitExceededException ex,
            HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("Rate limit exceeded for user operation", SeverityLevel.Information, properties);
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request));
    }
    
    /**
     * Handle IllegalArgumentException (fallback for validation errors in user service)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("Illegal argument in user operation", SeverityLevel.Information, properties);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }
}
