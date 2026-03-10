package com.fleebug.corerouter.exception.token;

/**
 * Exception thrown when a service token is not found by name or ID
 */
public class ServiceTokenNotFoundException extends RuntimeException {

    public ServiceTokenNotFoundException(String name) {
        super("Service token not found: " + name);
    }

    public ServiceTokenNotFoundException(Long id) {
        super("Service token not found with ID: " + id);
    }
}
