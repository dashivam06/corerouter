package com.fleebug.corerouter.exception.apikey;

/**
 * Exception thrown when an API key with the specified ID is not found
 */
public class ApiKeyNotFoundException extends RuntimeException {
    
    public ApiKeyNotFoundException(Integer apiKeyId) {
        super("API key with ID '" + apiKeyId + "' not found");
    }
    
    public ApiKeyNotFoundException(String key) {
        super("API key '" + key + "' not found");
    }
}
