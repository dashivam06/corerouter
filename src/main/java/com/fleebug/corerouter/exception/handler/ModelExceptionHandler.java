package com.fleebug.corerouter.exception.handler;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.exception.model.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Model domain exception handler
 * 
 * Handles all model-related exceptions and returns standardized error responses
 * with proper HTTP status codes and logging via SLF4J
 */
@RestControllerAdvice(basePackages = "com.fleebug.corerouter.controller.model")
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ModelExceptionHandler {
    
    /**
     * Handle ModelNotFoundException
     */
    @ExceptionHandler(ModelNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleModelNotFoundException(
            ModelNotFoundException ex,
            HttpServletRequest request) {
        log.warn("Model not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage(), request));
    }
    
    /**
     * Handle ModelAlreadyExistsException
     */
    @ExceptionHandler(ModelAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleModelAlreadyExistsException(
            ModelAlreadyExistsException ex,
            HttpServletRequest request) {
        log.warn("Model already exists: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(HttpStatus.CONFLICT, ex.getMessage(), request));
    }
    
    /**
     * Handle InvalidModelStatusException
     */
    @ExceptionHandler(InvalidModelStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidModelStatusException(
            InvalidModelStatusException ex,
            HttpServletRequest request) {
        log.warn("Invalid model status: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }
    
    /**
     * Handle IllegalArgumentException (fallback for validation errors in model service)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        log.warn("Illegal argument in model operation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }
}
