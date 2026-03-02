package com.fleebug.corerouter.exception.handler;

import com.fleebug.corerouter.dto.common.ErrorResponse;
import com.fleebug.corerouter.exception.apikey.ApiKeyNotFoundException;
import com.fleebug.corerouter.exception.apikey.ApiKeyRevokedException;
import com.fleebug.corerouter.exception.model.ModelNotFoundException;
import com.fleebug.corerouter.exception.task.TaskNotFoundException;
import com.fleebug.corerouter.exception.task.TaskInvalidStatusException;
import com.fleebug.corerouter.exception.task.TaskProcessingException;
import com.fleebug.corerouter.exception.task.TaskPayloadInvalidException;
import com.fleebug.corerouter.exception.task.TaskRetryExceededException;
import com.fleebug.corerouter.exception.task.TaskTimeoutException;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Task domain exception handler
 * 
 * Handles all task-related exceptions and returns standardized error responses
 * with proper HTTP status codes and logging via SLF4J
 */
@RestControllerAdvice(basePackages = {
        "com.fleebug.corerouter.controller.task",
        "com.fleebug.corerouter.controller.llm",
        "com.fleebug.corerouter.controller.ocr"
})
@Slf4j
public class TaskExceptionHandler {

    /**
     * Handle TaskNotFoundException
     */
    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotFoundException(
            TaskNotFoundException ex,
            HttpServletRequest request) {
        log.warn("Task not found: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .success(false)
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();
                
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle ApiKeyNotFoundException
     */
    @ExceptionHandler(ApiKeyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleApiKeyNotFoundException(
            ApiKeyNotFoundException ex,
            HttpServletRequest request) {
        log.warn("API key not found: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .success(false)
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();
                
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle ApiKeyRevokedException
     */
    @ExceptionHandler(ApiKeyRevokedException.class)
    public ResponseEntity<ErrorResponse> handleApiKeyRevokedException(
            ApiKeyRevokedException ex,
            HttpServletRequest request) {
        log.warn("API key not usable: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .success(false)
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();
                
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handle ModelNotFoundException
     */
    @ExceptionHandler(ModelNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleModelNotFoundException(
            ModelNotFoundException ex,
            HttpServletRequest request) {
        log.warn("Model not found: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .success(false)
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();
                
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle TaskInvalidStatusException
     */
    @ExceptionHandler(TaskInvalidStatusException.class)
    public ResponseEntity<ErrorResponse> handleTaskInvalidStatusException(
            TaskInvalidStatusException ex,
            HttpServletRequest request) {
        log.warn("Invalid task status operation: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .success(false)
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();
                
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle TaskTimeoutException
     */
    @ExceptionHandler(TaskTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTaskTimeoutException(
            TaskTimeoutException ex,
            HttpServletRequest request) {
        log.warn("Task timeout: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.REQUEST_TIMEOUT.value())
                .success(false)
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();
                
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResponse);
    }

    /**
     * Handle TaskRetryExceededException
     */
    @ExceptionHandler(TaskRetryExceededException.class)
    public ResponseEntity<ErrorResponse> handleTaskRetryExceededException(
            TaskRetryExceededException ex,
            HttpServletRequest request) {
        log.warn("Task retry limit exceeded: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .success(false)
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();
                
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }

    /**
     * Handle TaskProcessingException
     */
    @ExceptionHandler(TaskProcessingException.class)
    public ResponseEntity<ErrorResponse> handleTaskProcessingException(
            TaskProcessingException ex,
            HttpServletRequest request) {
        log.error("Task processing failed: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .success(false)
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();
                
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handle TaskPayloadInvalidException
     */
    @ExceptionHandler(TaskPayloadInvalidException.class)
    public ResponseEntity<ErrorResponse> handleTaskPayloadInvalidException(
            TaskPayloadInvalidException ex,
            HttpServletRequest request) {
        log.warn("Invalid task payload: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .success(false)
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();
                
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle JsonProcessingException
     */
    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<ErrorResponse> handleJsonProcessingException(
            JsonProcessingException ex,
            HttpServletRequest request) {
        log.warn("JSON processing error: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .success(false)
                .message("Invalid JSON format: " + ex.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();
                
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle IllegalArgumentException (fallback for validation errors in task operations)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        log.warn("Invalid argument in task operation: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .success(false)
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();
                
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
