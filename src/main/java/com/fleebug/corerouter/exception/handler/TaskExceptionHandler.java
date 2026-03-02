package com.fleebug.corerouter.exception.handler;

import com.fleebug.corerouter.dto.common.ApiResponse;
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
    public ResponseEntity<ApiResponse<Void>> handleTaskNotFoundException(
            TaskNotFoundException ex,
            HttpServletRequest request) {
        log.warn("Task not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage(), request));
    }

    /**
     * Handle ApiKeyNotFoundException
     */
    @ExceptionHandler(ApiKeyNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiKeyNotFoundException(
            ApiKeyNotFoundException ex,
            HttpServletRequest request) {
        log.warn("API key not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage(), request));
    }

    /**
     * Handle ApiKeyRevokedException
     */
    @ExceptionHandler(ApiKeyRevokedException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiKeyRevokedException(
            ApiKeyRevokedException ex,
            HttpServletRequest request) {
        log.warn("API key not usable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(HttpStatus.FORBIDDEN, ex.getMessage(), request));
    }

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
     * Handle TaskInvalidStatusException
     */
    @ExceptionHandler(TaskInvalidStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleTaskInvalidStatusException(
            TaskInvalidStatusException ex,
            HttpServletRequest request) {
        log.warn("Invalid task status operation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }

    /**
     * Handle TaskTimeoutException
     */
    @ExceptionHandler(TaskTimeoutException.class)
    public ResponseEntity<ApiResponse<Void>> handleTaskTimeoutException(
            TaskTimeoutException ex,
            HttpServletRequest request) {
        log.warn("Task timeout: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                .body(ApiResponse.error(HttpStatus.REQUEST_TIMEOUT, ex.getMessage(), request));
    }

    /**
     * Handle TaskRetryExceededException
     */
    @ExceptionHandler(TaskRetryExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleTaskRetryExceededException(
            TaskRetryExceededException ex,
            HttpServletRequest request) {
        log.warn("Task retry limit exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request));
    }

    /**
     * Handle TaskProcessingException
     */
    @ExceptionHandler(TaskProcessingException.class)
    public ResponseEntity<ApiResponse<Void>> handleTaskProcessingException(
            TaskProcessingException ex,
            HttpServletRequest request) {
        log.error("Task processing failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request));
    }

    /**
     * Handle TaskPayloadInvalidException
     */
    @ExceptionHandler(TaskPayloadInvalidException.class)
    public ResponseEntity<ApiResponse<Void>> handleTaskPayloadInvalidException(
            TaskPayloadInvalidException ex,
            HttpServletRequest request) {
        log.warn("Invalid task payload: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }

    /**
     * Handle JsonProcessingException
     */
    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<ApiResponse<Void>> handleJsonProcessingException(
            JsonProcessingException ex,
            HttpServletRequest request) {
        log.warn("JSON processing error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "Invalid JSON format: " + ex.getMessage(), request));
    }

    /**
     * Handle IllegalArgumentException (fallback for validation errors in task operations)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        log.warn("Invalid argument in task operation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }
}
