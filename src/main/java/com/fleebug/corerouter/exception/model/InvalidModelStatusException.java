package com.fleebug.corerouter.exception.model;

/**
 * Exception thrown when an invalid model status is provided
 */
public class InvalidModelStatusException extends RuntimeException {
    
    public InvalidModelStatusException(String status) {
        super("Invalid model status: '" + status + "'");
    }
    
    public InvalidModelStatusException() {
        super("Invalid model status provided");
    }
}
