package com.fleebug.corerouter.exception.handler;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.common.ErrorField;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * Main global exception handler
 * 
 * Catches all uncaught exceptions that are not handled by domain-specific handlers.
 * Includes:
 * - MethodArgumentNotValidException: Request body validation failures with field-level details
 * - NoHandlerFoundException: 404 errors for non-existent endpoints
 * - Exception: Generic fallback for any uncaught exceptions
 * 
 * All exceptions are logged via SLF4J and return standardized error responses
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
public class MainGlobalExceptionHandler {
    
    /**
     * Handle validation errors from @Valid annotation
     * 
     * Extracts field-level validation errors and returns them in a structured format
     * allowing clients to easily identify which fields failed validation and why
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        log.warn("Validation failed for request: {}", request.getRequestURI());
        
        List<ErrorField> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.add(ErrorField.builder()
                .field(error.getField())
                .message(error.getDefaultMessage())
                .build())
        );
        
        ApiResponse<Void> errorResponse = ApiResponse.error(
                HttpStatus.BAD_REQUEST,
                "Validation failed. Please check the errors field for details.",
                errors,
                request);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle 404 errors for non-existent endpoints
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            HttpServletRequest request) {
        log.warn("Endpoint not found: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        
        ApiResponse<Void> errorResponse = ApiResponse.error(
                HttpStatus.NOT_FOUND,
                "Endpoint not found. Please check your request URL.",
                request);
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Handle Redis connection failures during request processing
     * 
     * Catches RedisConnectionFailureException which occurs when Redis becomes unavailable
     * after application startup. Returns a user-friendly error message.
     * Admin can check server logs for detailed connection information.
     */
    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleRedisConnectionFailure(
            RedisConnectionFailureException ex,
            HttpServletRequest request) {
        log.error("Redis connection failed for request: {}. Error: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        ApiResponse<Void> errorResponse = ApiResponse.error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service temporarily unavailable. Cache service is not accessible. Please try again later.",
                request);
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }
    
    
    /**
     * Generic fallback handler for all uncaught exceptions
     * 
     * Logs the full exception stack trace for debugging purposes
     * Returns a generic error message without exposing internal details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        log.error("Uncaught exception occurred", ex);
        
        ApiResponse<Void> errorResponse = ApiResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                request);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
