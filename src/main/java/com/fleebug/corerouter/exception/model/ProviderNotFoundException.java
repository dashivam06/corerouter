package com.fleebug.corerouter.exception.model;

/**
 * Exception thrown when a provider with the specified ID or name is not found
 */
public class ProviderNotFoundException extends RuntimeException {
    
    public ProviderNotFoundException(Integer providerId) {
        super("Provider with ID '" + providerId + "' not found");
    }
    
    public ProviderNotFoundException(String field, String value) {
        super("Provider with " + field + " '" + value + "' not found");
    }
}
