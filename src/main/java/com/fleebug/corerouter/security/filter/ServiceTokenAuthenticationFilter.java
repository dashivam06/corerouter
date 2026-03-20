package com.fleebug.corerouter.security.filter;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fleebug.corerouter.entity.token.ServiceToken;
import com.fleebug.corerouter.exception.token.InvalidServiceTokenException;
import com.fleebug.corerouter.service.token.ServiceTokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String SERVICE_TOKEN_HEADER = "X-Service-Token";
    private static final String AUTH_ERROR_REASON_ATTR = "auth_error_reason";

    private final ServiceTokenService serviceTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String rawToken = request.getHeader(SERVICE_TOKEN_HEADER);

        // No service-token header → not a worker request, let other filters handle it
        if (rawToken == null || rawToken.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

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
            log.debug("Service token authenticated — name={}, role={}", serviceToken.getName(), serviceToken.getRole());
        } catch (InvalidServiceTokenException ex) {
            log.warn("Service token authentication failed: {}", ex.getMessage());
            request.setAttribute(AUTH_ERROR_REASON_ATTR, "invalid");
            // Don't set auth — Spring Security will reject with 401 downstream
        }

        filterChain.doFilter(request, response);
    }
}
