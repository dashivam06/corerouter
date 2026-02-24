package com.fleebug.corerouter.exception.model;

/**
 * Exception thrown when a model with the specified ID is not found
 */
public class ModelNotFoundException extends RuntimeException {
    
    public ModelNotFoundException(Integer modelId) {
        super("Model with ID '" + modelId + "' not found");
    }
    
    public ModelNotFoundException(String field, String value) {
        super("Model with " + field + " '" + value + "' not found");
    }
}
