package com.fleebug.corerouter.exception.handler;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import lombok.RequiredArgsConstructor;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.exception.token.InvalidServiceTokenException;
import com.fleebug.corerouter.exception.token.ServiceTokenAlreadyExistsException;
import com.fleebug.corerouter.exception.token.ServiceTokenNotFoundException;
import com.fleebug.corerouter.exception.token.ServiceTokenRevokedException;
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
 * Service token domain exception handler
 *
 * Handles all service-token-related exceptions thrown from service layer
 * and returns standardized error responses
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ServiceTokenExceptionHandler {

    private final TelemetryClient telemetryClient;

    @ExceptionHandler(InvalidServiceTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidServiceToken(
            InvalidServiceTokenException ex,
            HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("Invalid service token", SeverityLevel.Warning, properties);
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, ex.getMessage(), request));
    }

    @ExceptionHandler(ServiceTokenNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceTokenNotFound(
            ServiceTokenNotFoundException ex,
            HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("Service token not found", SeverityLevel.Warning, properties);
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage(), request));
    }

    @ExceptionHandler(ServiceTokenAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceTokenAlreadyExists(
            ServiceTokenAlreadyExistsException ex,
            HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("Service token conflict", SeverityLevel.Warning, properties);
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(HttpStatus.CONFLICT, ex.getMessage(), request));
    }

    @ExceptionHandler(ServiceTokenRevokedException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceTokenRevoked(
            ServiceTokenRevokedException ex,
            HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("Service token revoked", SeverityLevel.Warning, properties);
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(HttpStatus.FORBIDDEN, ex.getMessage(), request));
    }
}
