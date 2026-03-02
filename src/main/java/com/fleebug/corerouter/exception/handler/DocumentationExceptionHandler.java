package com.fleebug.corerouter.exception.handler;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.exception.documentation.DocumentationNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.fleebug.corerouter.controller.documentation")
@Slf4j
public class DocumentationExceptionHandler {

    @ExceptionHandler(DocumentationNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleDocumentationNotFound(
            DocumentationNotFoundException ex,
            HttpServletRequest request) {
        log.warn("Documentation not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage(), request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }
}
