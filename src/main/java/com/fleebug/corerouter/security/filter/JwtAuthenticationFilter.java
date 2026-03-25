package com.fleebug.corerouter.security.filter;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.fleebug.corerouter.enums.token.TokenValidationStatus;
import com.fleebug.corerouter.security.details.CustomUserDetails;
import com.fleebug.corerouter.security.jwt.JwtUtil;
import com.fleebug.corerouter.security.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_ERROR_REASON_ATTR = "auth_error_reason";

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final TelemetryClient telemetryClient;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String authorizationHeader = request.getHeader("Authorization");
            String token = extractTokenFromRequest(request);
            TokenValidationStatus validationStatus = null;

            if (authorizationHeader != null && !authorizationHeader.isBlank() && token == null) {
                request.setAttribute(AUTH_ERROR_REASON_ATTR, "invalid");
            }

            if (token != null) {
                validationStatus = jwtUtil.getTokenValidationStatus(token);

                if (validationStatus == TokenValidationStatus.EXPIRED) {
                    request.setAttribute(AUTH_ERROR_REASON_ATTR, "expired");
                } else if (validationStatus == TokenValidationStatus.INVALID) {
                    request.setAttribute(AUTH_ERROR_REASON_ATTR, "invalid");
                }
            }

            if (token != null && validationStatus == TokenValidationStatus.VALID) {
                String email = jwtUtil.extractEmailFromClaims(token);

                Map<String, String> properties = new HashMap<>();
                properties.put("email", email);
                telemetryClient.trackTrace("JWT validated", SeverityLevel.Verbose, properties);

                // Load user details from database using UserDetailsService
                CustomUserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Create authentication token with proper authorities
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(userDetails);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                telemetryClient.trackTrace("Authentication set for user", SeverityLevel.Verbose, properties);
            }
        } catch (Exception e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("error", e.getMessage());
            telemetryClient.trackTrace("Cannot set user authentication", SeverityLevel.Warning, properties);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header
     * 
     * @param request HTTP request
     * @return JWT token or null
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7).trim();
            return token.isBlank() ? null : token;
        }
        return null;
    }
}
