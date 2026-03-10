package com.fleebug.corerouter.exception.token;

/**
 * Exception thrown when a revoked or inactive service token is used
 */
public class ServiceTokenRevokedException extends RuntimeException {

    public ServiceTokenRevokedException(String name) {
        super("Service token '" + name + "' has been revoked");
    }
}
