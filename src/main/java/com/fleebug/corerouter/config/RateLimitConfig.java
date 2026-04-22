package com.fleebug.corerouter.config;

import io.github.bucket4j.Bandwidth;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class RateLimitConfig {

    // 5 OTP requests per IP per 10 minutes
    public Bandwidth otpIpBandwidth() {
        return Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofMinutes(10))
                .build();
    }

    // 3 OTP requests per email per 10 minutes
    public Bandwidth otpEmailBandwidth() {
        return Bandwidth.builder()
                .capacity(3)
                .refillIntervally(3, Duration.ofMinutes(10))
                .build();
    }

    // 10 login attempts per IP per minute
    public Bandwidth loginIpBandwidth() {
        return Bandwidth.builder()
                .capacity(10)
                .refillIntervally(10, Duration.ofMinutes(1))
                .build();
    }

    // 10 verify OTP attempts per IP per minute
    public Bandwidth verifyIpBandwidth() {
        return Bandwidth.builder()
                .capacity(10)
                .refillIntervally(10, Duration.ofMinutes(1))
                .build();
    }

    // 5 token refresh attempts per IP per minute
    public Bandwidth refreshIpBandwidth() {
        return Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofMinutes(1))
                .build();
    }

    // Task creation: 5 per authenticated user per minute (EXPENSIVE — strictly limited)
    public Bandwidth taskCreationUserBandwidth() {
        return Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofMinutes(1))
                .build();
    }

    // Chat completions: 10 per API key per minute
    public Bandwidth chatApiKeyBandwidth() {
        return Bandwidth.builder()
                .capacity(10)
                .refillIntervally(10, Duration.ofMinutes(1))
                .build();
    }

    // OCR image-to-text: 5 per API key per minute
    public Bandwidth ocrApiKeyBandwidth() {
        return Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofMinutes(1))
                .build();
    }

    // Speech-to-text job creation: 5 per API key per minute
    public Bandwidth speechApiKeyBandwidth() {
        return Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofMinutes(1))
                .build();
    }
}