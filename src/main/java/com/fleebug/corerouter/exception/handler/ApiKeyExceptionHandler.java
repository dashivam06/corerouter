package com.fleebug.corerouter.exception.handler;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.exception.apikey.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * API Key domain exception handler
 * 
 * Handles all API key-related exceptions and returns standardized error responses
 * with proper HTTP status codes and logging via SLF4J
 */
@RestControllerAdvice(basePackages = "com.fleebug.corerouter.controller.apikey")
@Slf4j
public class ApiKeyExceptionHandler {
    
    /**
     * Handle ApiKeyNotFoundException
     */
    @ExceptionHandler(ApiKeyNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiKeyNotFoundException(
            ApiKeyNotFoundException ex,
            HttpServletRequest request) {
        log.warn("API key not found: {}", ex.getMessage());
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
        log.warn("API key expired: {}", ex.getMessage());
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
        log.warn("API key revoked: {}", ex.getMessage());
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
        log.warn("Rate limit exceeded: {}", ex.getMessage());
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
        log.warn("Illegal argument in API key operation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }
}
