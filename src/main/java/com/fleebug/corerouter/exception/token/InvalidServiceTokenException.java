package com.fleebug.corerouter.exception.token;

/**
 * Exception thrown when a service token fails authentication —
 * either the token doesn't match any active record or has been revoked
 */
public class InvalidServiceTokenException extends RuntimeException {

    public InvalidServiceTokenException() {
        super("Invalid or inactive service token");
    }

    public InvalidServiceTokenException(String message) {
        super(message);
    }
}
