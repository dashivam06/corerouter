package com.fleebug.corerouter.exception.user;

/**
 * Exception thrown when attempting to create a user with an email that already exists
 */
public class UserAlreadyExistsException extends RuntimeException {
    
    public UserAlreadyExistsException(String email) {
        super("User with email '" + email + "' already exists");
    }
    
    public UserAlreadyExistsException() {
        super("User already exists");
    }
}
