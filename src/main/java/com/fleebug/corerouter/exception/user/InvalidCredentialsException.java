package com.fleebug.corerouter.exception.user;

/**
 * Exception thrown when user provides invalid credentials during login
 */
public class InvalidCredentialsException extends RuntimeException {
    
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
    
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
