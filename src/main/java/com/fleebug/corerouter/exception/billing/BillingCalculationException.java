package com.fleebug.corerouter.exception.billing;

public class BillingCalculationException extends RuntimeException {

    public BillingCalculationException(String message) {
        super(message);
    }

    public BillingCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
