package com.fleebug.corerouter.exception.handler;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import lombok.RequiredArgsConstructor;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.exception.apikey.*;
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
 * API Key domain exception handler
 * 
 * Handles all API key-related exceptions and returns standardized error responses
 * with proper HTTP status codes and logging via Azure Telemetry
 */
@RestControllerAdvice(basePackages = "com.fleebug.corerouter.controller.apikey")
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiKeyExceptionHandler {

    private final TelemetryClient telemetryClient;
    
    /**
     * Handle ApiKeyNotFoundException
     */
    @ExceptionHandler(ApiKeyNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiKeyNotFoundException(
            ApiKeyNotFoundException ex,
            HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("API key not found", SeverityLevel.Information, properties);
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage(), request));
    }
    
    /**
     * Handle ApiKeyExpiredException
     */
    @ExceptionHandler(ApiKeyExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiKeyExpiredException(
            ApiKeyExpiredException ex,
            HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("API key expired", SeverityLevel.Information, properties);
        
        return ResponseEntity.status(HttpStatus.GONE)
                .body(ApiResponse.error(HttpStatus.GONE, ex.getMessage(), request));
    }
    
    /**
     * Handle ApiKeyRevokedException
     */
    @ExceptionHandler(ApiKeyRevokedException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiKeyRevokedException(
            ApiKeyRevokedException ex,
            HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("API key revoked", SeverityLevel.Information, properties);
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(HttpStatus.FORBIDDEN, ex.getMessage(), request));
    }
    
    /**
     * Handle RateLimitExceededException
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitExceededException(
            RateLimitExceededException ex,
            HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("Rate limit exceeded for API key", SeverityLevel.Information, properties);
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request));
    }
    
    /**
     * Handle IllegalArgumentException (fallback for validation errors in apikey service)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("Illegal argument in API key operation", SeverityLevel.Information, properties);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }
}
