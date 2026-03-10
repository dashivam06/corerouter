package com.fleebug.corerouter.exception.token;

/**
 * Exception thrown when a service token with the given name already exists
 */
public class ServiceTokenAlreadyExistsException extends RuntimeException {

    public ServiceTokenAlreadyExistsException(String name) {
        super("Service token with name '" + name + "' already exists");
    }
}
