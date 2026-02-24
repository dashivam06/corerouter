package com.fleebug.corerouter.exception.user;

/**
 * Exception thrown when a user with the specified ID is not found
 */
public class UserNotFoundException extends RuntimeException {
    
    public UserNotFoundException(Integer userId) {
        super("User with ID '" + userId + "' not found");
    }
    
    public UserNotFoundException(String email) {
        super("User with email '" + email + "' not found");
    }
    
    public UserNotFoundException(String field, String value) {
        super("User with " + field + " '" + value + "' not found");
    }
}
