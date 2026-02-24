package com.fleebug.corerouter.exception.user;

/**
 * Exception thrown when invalid OTP code is provided
 */
public class InvalidOtpException extends RuntimeException {
    
    public InvalidOtpException() {
        super("Invalid OTP code. Please try again");
    }
    
    public InvalidOtpException(String message) {
        super(message);
    }
}
