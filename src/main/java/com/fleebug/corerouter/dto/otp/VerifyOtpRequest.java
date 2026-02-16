package com.fleebug.corerouter.dto.otp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;



@Getter
@Setter
public class VerifyOtpRequest {
    @NotBlank(message = "VerificationId is required")
    private String verificationId;
    
    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP must be 6 digits")
    private String otp;
}
