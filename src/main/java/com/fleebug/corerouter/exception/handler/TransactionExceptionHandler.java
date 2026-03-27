package com.fleebug.corerouter.exception.handler;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import lombok.RequiredArgsConstructor;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.exception.payment.InvalidTransactionAmountException;
import com.fleebug.corerouter.exception.payment.TransactionNotFoundException;
import com.fleebug.corerouter.exception.payment.TransactionVerificationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.fleebug.corerouter.controller.billing")
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransactionExceptionHandler {

    private final TelemetryClient telemetryClient;

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransactionNotFoundException(
            TransactionNotFoundException ex, HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("Transaction not found", SeverityLevel.Information, properties);
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage(), request));
    }

    @ExceptionHandler(InvalidTransactionAmountException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTransactionAmountException(
            InvalidTransactionAmountException ex, HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("Invalid transaction amount", SeverityLevel.Information, properties);
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }

    @ExceptionHandler(TransactionVerificationException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransactionVerificationException(
            TransactionVerificationException ex, HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("path", request.getRequestURI());
        properties.put("error", ex.getMessage());
        telemetryClient.trackTrace("Transaction verification failed", SeverityLevel.Information, properties);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }
}