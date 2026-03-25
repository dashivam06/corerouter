package com.fleebug.corerouter.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.fleebug.corerouter.constants.ApiPaths;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.security.details.CustomUserDetails;
import com.fleebug.corerouter.service.redis.RedisBucketService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting filter for task endpoints.
 * 
 * Limits:
 * - POST /v1/tasks (create): 5 per minute per authenticated user (EXPENSIVE — task processing is costly)
 */
@Component
public class TaskRateLimitFilter extends OncePerRequestFilter {

    @Autowired
    private RedisBucketService redisBucketService;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TelemetryClient telemetryClient;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if (HttpMethod.POST.matches(method) && path.equals(ApiPaths.TASKS)) {
            handleTaskCreationRateLimit(request, response, filterChain);
        } else {
            filterChain.doFilter(request, response);
        }
    }

            private void handleTaskCreationRateLimit(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String userKey = resolveAuthenticatedUserKey();
        if (userKey == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = redisBucketService.resolveTaskCreationUserBucket(userKey);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long retryAfter = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            
            Map<String, String> properties = new HashMap<>();
            properties.put("userKey", userKey);
            properties.put("retryAfter", String.valueOf(retryAfter));
            telemetryClient.trackTrace("Task creation rate limit exceeded", SeverityLevel.Warning, properties);
            
            writeTooManyRequests(response, request,
                    "Task creation quota exceeded (5 per minute). Processing is expensive. Retry after " + retryAfter + "s");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveAuthenticatedUserKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomUserDetails customUserDetails) {
                return "uid:" + customUserDetails.getUserId();
            }
            if (principal instanceof String principalName && !principalName.isBlank()) {
                return "user:" + principalName;
            }
        }

        return null;
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
