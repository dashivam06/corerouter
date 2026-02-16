package com.fleebug.corerouter.dto.otp;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Builder
public class RequestOtpResponse {
    private String verificationId;        
    private String message;            
    private int ttlMinutes;            
}
