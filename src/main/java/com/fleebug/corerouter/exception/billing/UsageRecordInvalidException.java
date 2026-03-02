package com.fleebug.corerouter.exception.billing;

public class UsageRecordInvalidException extends RuntimeException {

    public UsageRecordInvalidException(String message) {
        super(message);
    }

    public UsageRecordInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
