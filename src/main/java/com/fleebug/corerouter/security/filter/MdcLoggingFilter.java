package com.fleebug.corerouter.security.filter;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class MdcLoggingFilter extends OncePerRequestFilter {

    private final TelemetryClient telemetryClient;

    private static final String HEADER_REQUEST_ID = "X-Request-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String requestId = request.getHeader(HEADER_REQUEST_ID);
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }

        // Add requestId to response header for client tracking
        response.setHeader(HEADER_REQUEST_ID, requestId);

        Map<String, String> properties = new HashMap<>();
        properties.put("requestId", requestId);
        properties.put("method", request.getMethod());
        properties.put("path", request.getRequestURI());
        properties.put("remoteAddr", request.getRemoteAddr());
        properties.put("userAgent", request.getHeader("User-Agent"));

        telemetryClient.trackTrace("Request started", SeverityLevel.Verbose, properties);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            properties.put("status", String.valueOf(response.getStatus()));
            properties.put("durationMs", String.valueOf(duration));
            
            telemetryClient.trackTrace("Request finished", SeverityLevel.Verbose, properties);
        }
    }
}
