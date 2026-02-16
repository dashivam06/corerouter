package com.fleebug.corerouter.dto.otp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal cache data structure for storing pending registration with OTP
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpCacheData {

    private String email;
    
    @JsonProperty("registration_data")
    private String registrationData;
    
    @JsonProperty("created_at")
    private long createdAt;
}
