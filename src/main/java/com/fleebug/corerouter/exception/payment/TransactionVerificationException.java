package com.fleebug.corerouter.exception.payment;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TransactionVerificationException extends RuntimeException {
    public TransactionVerificationException(String message) {
        super(message);
    }

    public TransactionVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}