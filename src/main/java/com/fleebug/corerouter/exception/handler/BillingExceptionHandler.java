package com.fleebug.corerouter.exception.handler;

import com.fleebug.corerouter.dto.common.ErrorResponse;
import com.fleebug.corerouter.exception.billing.BillingCalculationException;
import com.fleebug.corerouter.exception.billing.BillingConfigNotFoundException;
import com.fleebug.corerouter.exception.billing.UsageRecordInvalidException;
import com.fleebug.corerouter.exception.model.ModelNotFoundException;
import com.fleebug.corerouter.exception.task.TaskNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice(basePackages = "com.fleebug.corerouter.controller.billing")
@Slf4j
public class BillingExceptionHandler {

    @ExceptionHandler(BillingConfigNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBillingConfigNotFoundException(
            BillingConfigNotFoundException ex,
            HttpServletRequest request) {
        log.warn("Billing config not found: {}", ex.getMessage());

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

    @ExceptionHandler(BillingCalculationException.class)
    public ResponseEntity<ErrorResponse> handleBillingCalculationException(
            BillingCalculationException ex,
            HttpServletRequest request) {
        log.error("Billing calculation failed: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .success(false)
                .message("Billing calculation failed: " + ex.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(UsageRecordInvalidException.class)
    public ResponseEntity<ErrorResponse> handleUsageRecordInvalidException(
            UsageRecordInvalidException ex,
            HttpServletRequest request) {
        log.warn("Invalid usage record: {}", ex.getMessage());

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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        log.warn("Bad request: {}", ex.getMessage());

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
