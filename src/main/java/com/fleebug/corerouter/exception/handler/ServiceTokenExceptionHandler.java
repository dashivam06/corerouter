package com.fleebug.corerouter.exception.handler;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.exception.token.InvalidServiceTokenException;
import com.fleebug.corerouter.exception.token.ServiceTokenAlreadyExistsException;
import com.fleebug.corerouter.exception.token.ServiceTokenNotFoundException;
import com.fleebug.corerouter.exception.token.ServiceTokenRevokedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Service token domain exception handler
 *
 * Handles all service-token-related exceptions thrown from service layer
 * and returns standardized error responses
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ServiceTokenExceptionHandler {

    @ExceptionHandler(InvalidServiceTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidServiceToken(
            InvalidServiceTokenException ex,
            HttpServletRequest request) {
        log.warn("Invalid service token: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, ex.getMessage(), request));
    }

    @ExceptionHandler(ServiceTokenNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceTokenNotFound(
            ServiceTokenNotFoundException ex,
            HttpServletRequest request) {
        log.warn("Service token not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage(), request));
    }

    @ExceptionHandler(ServiceTokenAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceTokenAlreadyExists(
            ServiceTokenAlreadyExistsException ex,
            HttpServletRequest request) {
        log.warn("Service token conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(HttpStatus.CONFLICT, ex.getMessage(), request));
    }

    @ExceptionHandler(ServiceTokenRevokedException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceTokenRevoked(
            ServiceTokenRevokedException ex,
            HttpServletRequest request) {
        log.warn("Service token revoked: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(HttpStatus.FORBIDDEN, ex.getMessage(), request));
    }
}
