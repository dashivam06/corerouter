package com.fleebug.corerouter.security.filter;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleebug.corerouter.constants.ApiPaths;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.service.apikey.ApiKeyService;
import com.fleebug.corerouter.service.redis.RedisBucketService;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatRateLimitFilter extends OncePerRequestFilter {

    private final RedisBucketService redisBucketService;
    private final ApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;
    private final TelemetryClient telemetryClient;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Only apply to POST /api/v1/chat/completions
        if (HttpMethod.POST.matches(method) && path.equals(ApiPaths.CHAT_COMPLETIONS)) {
            String token = extractToken(request);
            if (token != null) {
                // Extract userId from API key format (cr_live_<userId>_<random>)
                Integer userId = extractUserIdFromApiKey(token);
                if (userId != null) {
                    Map<String, String> context = new HashMap<>();
                    context.put("userId", String.valueOf(userId));
                    telemetryClient.trackTrace("Chat request from user", SeverityLevel.Verbose, context);
                }

                // We use the HASH of the API key as the bucket identifier
                // This ensures we limit based on the actual key, even if rotated or different raw values map to same (unlikely)
                String apiKeyHash = apiKeyService.hashKey(token);
                
                Bucket bucket = redisBucketService.resolveChatApiKeyBucket(apiKeyHash);
                ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

                if (!probe.isConsumed()) {
                    long retryAfter = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;
                    response.setHeader("Retry-After", String.valueOf(retryAfter));
                    
                    Map<String, String> properties = new HashMap<>();
                    properties.put("apiKeyHash", apiKeyHash);
                    properties.put("retryAfter", String.valueOf(retryAfter));
                    telemetryClient.trackTrace("Chat rate limit exceeded", SeverityLevel.Warning, properties);
                    
                    writeTooManyRequests(response, request, "Too many requests. Please try again in " + retryAfter + " seconds.");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private Integer extractUserIdFromApiKey(String apiKey) {
        if (apiKey == null || !apiKey.startsWith("cr_live_")) {
            return null;
        }
        try {
            String[] parts = apiKey.split("_");
            if (parts.length >= 3) {
                return Integer.parseInt(parts[2]);
            }
        } catch (NumberFormatException e) {
            Map<String, String> properties = new HashMap<>();
            properties.put("apiKey", apiKey);
            telemetryClient.trackTrace("Failed to extract user ID from API key", SeverityLevel.Warning, properties);
        }
        return null;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    private void writeTooManyRequests(HttpServletResponse response, HttpServletRequest request, String message) throws IOException {
        ApiResponse<Void> body = ApiResponse.error(HttpStatus.TOO_MANY_REQUESTS, message, request);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
