package com.fleebug.corerouter.exception.billing;

public class BillingConfigNotFoundException extends RuntimeException {

    public BillingConfigNotFoundException(Integer modelId) {
        super("Billing config for model ID '" + modelId + "' not found");
    }

    public BillingConfigNotFoundException(String message) {
        super(message);
    }
}
