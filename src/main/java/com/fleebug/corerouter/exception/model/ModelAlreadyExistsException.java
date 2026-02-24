package com.fleebug.corerouter.exception.model;

/**
 * Exception thrown when attempting to create a model that already exists
 */
public class ModelAlreadyExistsException extends RuntimeException {
    
    public ModelAlreadyExistsException(String fullname) {
        super("Model with name '" + fullname + "' already exists");
    }
    
    public ModelAlreadyExistsException() {
        super("Model already exists");
    }
}
