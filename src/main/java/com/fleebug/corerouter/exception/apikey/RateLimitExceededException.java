package com.fleebug.corerouter.exception.apikey;

/**
 * Exception thrown when API rate limit has been exceeded
 */
public class RateLimitExceededException extends RuntimeException {
    
    public RateLimitExceededException() {
        super("Rate limit exceeded. Please try again later");
    }
    
    public RateLimitExceededException(String message) {
        super(message);
    }
}
