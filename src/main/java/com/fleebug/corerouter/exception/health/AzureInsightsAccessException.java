package com.fleebug.corerouter.exception.health;

public class AzureInsightsAccessException extends RuntimeException {

    public AzureInsightsAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}