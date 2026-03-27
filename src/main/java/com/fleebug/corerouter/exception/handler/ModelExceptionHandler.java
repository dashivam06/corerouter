package com.fleebug.corerouter.exception.handler;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import lombok.RequiredArgsConstructor;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.exception.model.*;
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
 * Model domain exception handler
 * 
 * Handles all model-related exceptions and returns standardized error responses
 * with proper HTTP status codes and logging via Azure Telemetry
 */
@RestControllerAdvice(basePackages = "com.fleebug.corerouter.controller.model")
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ModelExceptionHandler {

    private final TelemetryClient telemetryClient;
    
    /**
     * Handle ModelNotFoundException
     */
    @ExceptionHandler(ModelNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleModelNotFoundException(
            ModelNotFoundException ex,
            HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("Model not found", SeverityLevel.Information, properties);
        
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
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("Model already exists", SeverityLevel.Information, properties);
        
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
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("Invalid model status", SeverityLevel.Information, properties);
        
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
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("Illegal argument in model operation", SeverityLevel.Information, properties);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }
}
