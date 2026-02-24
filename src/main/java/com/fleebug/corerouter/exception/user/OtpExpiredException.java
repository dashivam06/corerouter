package com.fleebug.corerouter.exception.user;

/**
 * Exception thrown when OTP has expired
 */
public class OtpExpiredException extends RuntimeException {
    
    public OtpExpiredException() {
        super("OTP has expired. Please request a new one");
    }
    
    public OtpExpiredException(String message) {
        super(message);
    }
}
