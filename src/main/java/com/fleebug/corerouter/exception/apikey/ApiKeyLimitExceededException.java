package com.fleebug.corerouter.exception.apikey;

public class ApiKeyLimitExceededException extends RuntimeException {
    public ApiKeyLimitExceededException(String message) {
        super(message);
    }
}