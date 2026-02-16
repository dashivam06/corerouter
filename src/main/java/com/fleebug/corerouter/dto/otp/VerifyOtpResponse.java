package com.fleebug.corerouter.dto.otp;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Builder
public class VerifyOtpResponse {
    private String verificationId;        
    private String message;               
    private boolean verified;          
    private int profileCompletionTtlMinutes; 
}
