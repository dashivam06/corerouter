package com.fleebug.corerouter.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String MDC_KEY_REQUEST_ID = "requestId";
    private static final String MDC_KEY_METHOD = "method";
    private static final String MDC_KEY_PATH = "path";
    private static final String MDC_KEY_REMOTE_ADDR = "remoteAddr";
    private static final String MDC_KEY_USER_AGENT = "userAgent";
    private static final String HEADER_REQUEST_ID = "X-Request-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        try {
            // Generate or extract Request ID
            String requestId = request.getHeader(HEADER_REQUEST_ID);
            if (requestId == null || requestId.isEmpty()) {
                requestId = UUID.randomUUID().toString();
            }

            // Put into MDC
            MDC.put(MDC_KEY_REQUEST_ID, requestId);
            MDC.put(MDC_KEY_METHOD, request.getMethod());
            MDC.put(MDC_KEY_PATH, request.getRequestURI());
            MDC.put(MDC_KEY_REMOTE_ADDR, request.getRemoteAddr());
            MDC.put(MDC_KEY_USER_AGENT, request.getHeader("User-Agent"));

            // Add requestId to response header for client tracking
            response.setHeader(HEADER_REQUEST_ID, requestId);

            log.debug("Request started: {} {}", request.getMethod(), request.getRequestURI());

            filterChain.doFilter(request, response);

        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Request finished: {} {} - Status: {} - Duration: {}ms", 
                    request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
            
            // Clear MDC to prevent thread reuse pollution
            MDC.clear();
        }
    }
}
