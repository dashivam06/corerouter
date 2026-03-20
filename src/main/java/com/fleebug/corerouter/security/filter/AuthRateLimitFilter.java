package com.fleebug.corerouter.security.filter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.service.redis.RedisBucketService;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    @Autowired
    private RedisBucketService redisBucketService;
    
    @Autowired
    private  ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if ("/api/v1/auth/request-otp".equals(path)) {
            handleOtpRateLimit(request, response, filterChain);
        } else if ("/login".equals(path)) {
            handleLoginRateLimit(request, response, filterChain);
        } else {
            filterChain.doFilter(request, response);
        }

    }

    private void handleOtpRateLimit(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = resolveClientIp(request);

        Bucket ipBucket = redisBucketService.resolveOtpIpBucket(clientIp);
        ConsumptionProbe ipProbe = ipBucket.tryConsumeAndReturnRemaining(1);

        if (!ipProbe.isConsumed()) {
            long retryAfter = TimeUnit.NANOSECONDS.toSeconds(ipProbe.getNanosToWaitForRefill()) + 1;
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            writeTooManyRequests(response, request,
                    "Too many OTP requests from your IP. Retry after " + retryAfter + "s");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void handleLoginRateLimit(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = resolveClientIp(request);

        Bucket loginBucket = redisBucketService.resolveLoginIpBucket(clientIp);
        ConsumptionProbe probe = loginBucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long retryAfter = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            writeTooManyRequests(response, request,
                    "Too many login attempts. Retry after " + retryAfter + "s");
            return;
        }

        filterChain.doFilter(request, response);
    }
    

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank())
            return xff.split(",")[0].trim();

        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank())
            return xri.trim();

        return request.getRemoteAddr();
    }


     private void writeTooManyRequests(HttpServletResponse response,
            HttpServletRequest request,
            String message) throws IOException {
        ApiResponse<Void> body = ApiResponse.error(HttpStatus.TOO_MANY_REQUESTS, message, request);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

}
