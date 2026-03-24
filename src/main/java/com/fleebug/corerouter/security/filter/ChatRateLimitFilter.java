package com.fleebug.corerouter.security.filter;

import java.io.IOException;
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
import lombok.extern.slf4j.Slf4j;

import org.slf4j.MDC;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRateLimitFilter extends OncePerRequestFilter {

    private final RedisBucketService redisBucketService;
    private static final String MDC_KEY_USER_ID = "userId";
    private final ApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Only apply to POST /api/v1/chat/completions
        if (HttpMethod.POST.matches(method) && ApiPaths.CHAT_COMPLETIONS.equals(path)) {
            String token = extractToken(request);
            if (token != null) {
                // Extract userId from API key format (cr_live_<userId>_<random>)
                Integer userId = extractUserIdFromApiKey(token);
                if (userId != null) {
                    MDC.put(MDC_KEY_USER_ID, String.valueOf(userId));
                    log.debug("Chat request from user ID: {}", userId);
                }

                // We use the HASH of the API key as the bucket identifier
                // This ensures we limit based on the actual key, even if rotated or different raw values map to same (unlikely)
                String apiKeyHash = apiKeyService.hashKey(token);
                
                Bucket bucket = redisBucketService.resolveChatApiKeyBucket(apiKeyHash);
                ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

                if (!probe.isConsumed()) {
                    long retryAfter = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;
                    response.setHeader("Retry-After", String.valueOf(retryAfter));
                    log.warn("Rate limit exceeded for API Key hash: {}", apiKeyHash);
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
            log.warn("Failed to extract user ID from API key: {}", apiKey);
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
