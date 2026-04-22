package com.fleebug.corerouter.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OcrRateLimitFilter extends OncePerRequestFilter {

    private final RedisBucketService redisBucketService;
    private final ApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;
    private final TelemetryClient telemetryClient;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if (HttpMethod.POST.matches(method) && path.equals(ApiPaths.OCR_PARSE_IMAGE_URL)) {
            String token = extractToken(request);
            if (token != null) {
                String apiKeyHash = apiKeyService.hashKey(token);
                Bucket bucket = redisBucketService.resolveOcrApiKeyBucket(apiKeyHash);
                ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

                if (!probe.isConsumed()) {
                    long retryAfter = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;
                    response.setHeader("Retry-After", String.valueOf(retryAfter));

                    Map<String, String> properties = new HashMap<>();
                    properties.put("apiKeyHash", apiKeyHash);
                    properties.put("retryAfter", String.valueOf(retryAfter));
                    telemetryClient.trackTrace("OCR rate limit exceeded", SeverityLevel.Information, properties);

                    writeTooManyRequests(response, request,
                            "OCR rate limit exceeded (5 requests per minute). Retry after " + retryAfter + " seconds.");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    private void writeTooManyRequests(HttpServletResponse response, HttpServletRequest request, String message)
            throws IOException {
        ApiResponse<Void> body = ApiResponse.error(HttpStatus.TOO_MANY_REQUESTS, message, request);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}