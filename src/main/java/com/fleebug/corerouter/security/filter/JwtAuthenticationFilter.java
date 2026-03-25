package com.fleebug.corerouter.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fleebug.corerouter.enums.token.TokenValidationStatus;
import com.fleebug.corerouter.security.details.CustomUserDetails;
import com.fleebug.corerouter.security.jwt.JwtUtil;
import com.fleebug.corerouter.security.service.CustomUserDetailsService;

import java.io.IOException;

import org.slf4j.MDC;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_ERROR_REASON_ATTR = "auth_error_reason";
    private static final String MDC_KEY_USER_ID = "userId";

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

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

                log.debug("JWT validated for user: {}", email);

                // Load user details from database using UserDetailsService
                CustomUserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Create authentication token with proper authorities
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(userDetails);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // Add userId to MDC for logging context
                if (email != null) {
                    MDC.put(MDC_KEY_USER_ID, userDetails.getUserId().toString());
                }
                
                log.debug("Authentication set for user: {}", email);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
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
