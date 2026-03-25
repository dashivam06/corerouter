package com.fleebug.corerouter.security.filter;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.fleebug.corerouter.entity.token.ServiceToken;
import com.fleebug.corerouter.exception.token.InvalidServiceTokenException;
import com.fleebug.corerouter.service.token.ServiceTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ServiceTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String SERVICE_TOKEN_HEADER = "X-Service-Token";
    private static final String AUTH_ERROR_REASON_ATTR = "auth_error_reason";

    private final ServiceTokenService serviceTokenService;
    private final TelemetryClient telemetryClient;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String rawToken = request.getHeader(SERVICE_TOKEN_HEADER);

        if (rawToken != null && !rawToken.isBlank()) {
            try {
                ServiceToken serviceToken = serviceTokenService.authenticate(rawToken);

                // Map ServiceRole directly → ROLE_WORKER, ROLE_ADMIN, etc.
                String authority = "ROLE_" + serviceToken.getRole().name();
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                serviceToken.getName(),
                                null,
                                Collections.singleton(new SimpleGrantedAuthority(authority)));
                authentication.setDetails(serviceToken);

                SecurityContextHolder.getContext().setAuthentication(authentication);

                Map<String, String> properties = new HashMap<>();
                properties.put("serviceName", serviceToken.getName());
                properties.put("role", serviceToken.getRole().name());
                telemetryClient.trackTrace("Service token authenticated", SeverityLevel.Verbose, properties);

            } catch (InvalidServiceTokenException ex) {
                Map<String, String> properties = new HashMap<>();
                properties.put("error", ex.getMessage());
                telemetryClient.trackTrace("Service token authentication failed", SeverityLevel.Warning, properties);
                
                request.setAttribute(AUTH_ERROR_REASON_ATTR, "invalid");
                // Don't set auth — Spring Security will reject with 401 downstream
            }
        }

        filterChain.doFilter(request, response);
    }
}
