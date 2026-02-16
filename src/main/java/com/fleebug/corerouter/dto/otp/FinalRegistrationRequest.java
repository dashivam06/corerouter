package com.fleebug.corerouter.dto.otp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class FinalRegistrationRequest {
    @NotBlank(message = "VerificationId is required")
    private String verificationId;        // Proof token from step 2
    
    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
    private String fullName;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
    
    private String profileImage;          // Optional
    private boolean emailSubscribed;      // Newsletter subscription
}
