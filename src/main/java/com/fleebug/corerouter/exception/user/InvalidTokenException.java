package com.fleebug.corerouter.exception.user;

/**
 * Exception thrown when authentication token is invalid or has expired
 */
public class InvalidTokenException extends RuntimeException {
    
    public InvalidTokenException() {
        super("Invalid or expired authentication token");
    }
    
    public InvalidTokenException(String message) {
        super(message);
    }
}
