package com.fleebug.corerouter.exception.apikey;

/**
 * Exception thrown when attempting to use a revoked API key
 */
public class ApiKeyRevokedException extends RuntimeException {
    
    public ApiKeyRevokedException() {
        super("API key has been revoked and cannot be used");
    }
    
    public ApiKeyRevokedException(String message) {
        super(message);
    }
}
