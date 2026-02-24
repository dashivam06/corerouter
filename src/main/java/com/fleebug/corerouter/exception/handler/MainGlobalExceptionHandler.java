package com.fleebug.corerouter.exception.handler;

import com.fleebug.corerouter.dto.common.ErrorField;
import com.fleebug.corerouter.dto.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
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
@Slf4j
public class MainGlobalExceptionHandler {
    
    /**
     * Handle validation errors from @Valid annotation
     * 
     * Extracts field-level validation errors and returns them in a structured format
     * allowing clients to easily identify which fields failed validation and why
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
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
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .success(false)
                .message("Validation failed. Please check the errors field for details.")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .errors(errors)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle 404 errors for non-existent endpoints
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            HttpServletRequest request) {
        log.warn("Endpoint not found: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .success(false)
                .message("Endpoint not found. Please check your request URL.")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Generic fallback handler for all uncaught exceptions
     * 
     * Logs the full exception stack trace for debugging purposes
     * Returns a generic error message without exposing internal details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        log.error("Uncaught exception occurred", ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .success(false)
                .message("An unexpected error occurred. Please try again later.")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
