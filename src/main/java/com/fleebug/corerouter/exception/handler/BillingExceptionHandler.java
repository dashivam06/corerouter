package com.fleebug.corerouter.exception.handler;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.exception.billing.BillingCalculationException;
import com.fleebug.corerouter.exception.billing.BillingConfigNotFoundException;
import com.fleebug.corerouter.exception.billing.UsageRecordInvalidException;
import com.fleebug.corerouter.exception.model.ModelNotFoundException;
import com.fleebug.corerouter.exception.task.TaskNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.fleebug.corerouter.controller.billing")
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class BillingExceptionHandler {

    @ExceptionHandler(BillingConfigNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleBillingConfigNotFoundException(
            BillingConfigNotFoundException ex,
            HttpServletRequest request) {
        log.warn("Billing config not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage(), request));
    }

    @ExceptionHandler(BillingCalculationException.class)
    public ResponseEntity<ApiResponse<Void>> handleBillingCalculationException(
            BillingCalculationException ex,
            HttpServletRequest request) {
        log.error("Billing calculation failed: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "Billing calculation failed: " + ex.getMessage(), request));
    }

    @ExceptionHandler(UsageRecordInvalidException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsageRecordInvalidException(
            UsageRecordInvalidException ex,
            HttpServletRequest request) {
        log.warn("Invalid usage record: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTaskNotFoundException(
            TaskNotFoundException ex,
            HttpServletRequest request) {
        log.warn("Task not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage(), request));
    }

    @ExceptionHandler(ModelNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleModelNotFoundException(
            ModelNotFoundException ex,
            HttpServletRequest request) {
        log.warn("Model not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage(), request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }
}
