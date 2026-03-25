package com.fleebug.corerouter.security.service;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.repository.user.UserRepository;
import com.fleebug.corerouter.security.details.CustomUserDetails;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom UserDetailsService implementation for Spring Security
 * Loads user details from database based on email
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final TelemetryClient telemetryClient;

    @Override
    public CustomUserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Map<String, String> properties = new HashMap<>();
        properties.put("email", email);
        telemetryClient.trackTrace("Loading user details for email", SeverityLevel.Verbose, properties);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    telemetryClient.trackTrace("User not found with email", SeverityLevel.Warning, properties);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        telemetryClient.trackTrace("User details loaded successfully", SeverityLevel.Verbose, properties);
        return new CustomUserDetails(user);
    }
}
