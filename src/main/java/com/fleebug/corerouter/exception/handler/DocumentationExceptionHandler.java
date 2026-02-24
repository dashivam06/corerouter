package com.fleebug.corerouter.exception.handler;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.exception.documentation.DocumentationNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice(basePackages = "com.fleebug.corerouter.controller.documentation")
@Slf4j
public class DocumentationExceptionHandler {

    @ExceptionHandler(DocumentationNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleDocumentationNotFound(
            DocumentationNotFoundException ex,
            HttpServletRequest request) {
        log.warn("Documentation not found: {}", ex.getMessage());

        ApiResponse<Void> errorResponse = ApiResponse.<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .success(false)
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(null)
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());

        ApiResponse<Void> errorResponse = ApiResponse.<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .success(false)
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(null)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
