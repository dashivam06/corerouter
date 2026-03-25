package com.fleebug.corerouter.exception.handler;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.common.ErrorField;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import com.fleebug.corerouter.exception.apikey.RateLimitExceededException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main global exception handler
 * 
 * Catches all uncaught exceptions that are not handled by domain-specific handlers.
 * Includes:
 * - MethodArgumentNotValidException: Request body validation failures with field-level details
 * - NoHandlerFoundException: 404 errors for non-existent endpoints
 * - Exception: Generic fallback for any uncaught exceptions
 * 
 * All exceptions allow standardized error responses and telemetry tracking
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class MainGlobalExceptionHandler {

    private final TelemetryClient telemetryClient;
    
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
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("Validation failed", SeverityLevel.Warning, properties);
        
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
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("method", request.getMethod());
        telemetryClient.trackTrace("Endpoint not found", SeverityLevel.Warning, properties);
        
        ApiResponse<Void> errorResponse = ApiResponse.error(
                HttpStatus.NOT_FOUND,
                "Endpoint not found. Please check your request URL.",
                request);
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Handle rate limit exceeded errors
     * 
     * Catches RateLimitExceededException thrown when request rate limits are exceeded.
     * Returns 429 Too Many Requests status.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitExceededException(
            RateLimitExceededException ex,
            HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("message", ex.getMessage());
        telemetryClient.trackTrace("Rate limit exceeded", SeverityLevel.Warning, properties);
        
        ApiResponse<Void> errorResponse = ApiResponse.error(
                HttpStatus.TOO_MANY_REQUESTS,
                ex.getMessage(),
                request);
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }


        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiResponse<Void>> handleMissingBody(HttpMessageNotReadableException ex,
                                                                HttpServletRequest request) {
                Map<String, String> properties = new HashMap<>();
                properties.put("path", request.getRequestURI());
                properties.put("error", ex.getMessage());
                telemetryClient.trackTrace("Request body missing or malformed", SeverityLevel.Warning, properties);

                ApiResponse<Void> response =  ApiResponse.error(HttpStatus.BAD_REQUEST, "Request body is missing or malformed", request);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
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
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackException(ex, properties, null);
        
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
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        telemetryClient.trackException(ex, properties, null);
        
        ApiResponse<Void> errorResponse = ApiResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                request);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
