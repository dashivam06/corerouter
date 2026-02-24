package com.fleebug.corerouter.exception.apikey;

/**
 * Exception thrown when API key has expired
 */
public class ApiKeyExpiredException extends RuntimeException {
    
    public ApiKeyExpiredException() {
        super("API key has expired");
    }
    
    public ApiKeyExpiredException(String message) {
        super(message);
    }
}
