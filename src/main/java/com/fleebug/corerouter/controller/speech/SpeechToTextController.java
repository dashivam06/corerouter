package com.fleebug.corerouter.controller.speech;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.entity.apikey.ApiKey;
import com.fleebug.corerouter.enums.apikey.ApiKeyStatus;
import com.fleebug.corerouter.repository.apikey.ApiKeyRepository;
import com.fleebug.corerouter.service.apikey.ApiKeyService;
import com.fleebug.corerouter.service.speech.SpeechToTextService;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/speech-to-text")
@RequiredArgsConstructor
@Tag(name = "Speech To Text", description = "Speech-to-text endpoints")
public class SpeechToTextController {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SpeechToTextService speechToTextService;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyService apiKeyService;
    private final TelemetryClient telemetryClient;

    @Operation(
            summary = "Create speech-to-text job",
            description = "Forwards request payload to Rev AI jobs endpoint. Requires 'Authorization: Bearer <API_KEY>' header."
    )
    @PostMapping("/jobs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createJob(
            @RequestBody Map<String, Object> requestPayload,
            HttpServletRequest request) {

        Map<String, String> properties = new HashMap<>();
        properties.put("provider", "rev.ai");
        telemetryClient.trackTrace("Speech-to-text create job request", SeverityLevel.Information, properties);

        requireApiKey(request);
        validatePayload(requestPayload);

        Map<String, Object> providerResponse = speechToTextService.createJob(requestPayload);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Speech-to-text job created", providerResponse, request));
    }

    @Operation(
            summary = "Get speech-to-text job status",
            description = "Fetches Rev AI job details by job ID using your own backend endpoint. Requires 'Authorization: Bearer <API_KEY>' header."
    )
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getJob(
            @PathVariable String jobId,
            HttpServletRequest request) {

        requireApiKey(request);
        Map<String, Object> providerResponse = speechToTextService.getJob(jobId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Speech-to-text job fetched", providerResponse, request));
    }

    @Operation(
            summary = "Get speech-to-text transcript",
            description = "Fetches Rev AI transcript JSON by job ID using your own backend endpoint. Requires 'Authorization: Bearer <API_KEY>' header."
    )
    @GetMapping("/jobs/{jobId}/transcript")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTranscript(
            @PathVariable String jobId,
            HttpServletRequest request) {

        requireApiKey(request);
        Map<String, Object> providerResponse = speechToTextService.getTranscript(jobId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Speech-to-text transcript fetched", providerResponse, request));
    }

    @Operation(
            summary = "Get speech-to-text plain text",
            description = "Fetches Rev AI transcript and returns extracted text by job ID. Requires 'Authorization: Bearer <API_KEY>' header."
    )
    @GetMapping("/jobs/{jobId}/text")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTranscriptText(
            @PathVariable String jobId,
            HttpServletRequest request) {

        requireApiKey(request);
        Map<String, Object> providerResponse = speechToTextService.getTranscriptText(jobId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Speech-to-text text fetched", providerResponse, request));
    }

    private void validatePayload(Map<String, Object> requestPayload) {
        if (requestPayload == null || requestPayload.isEmpty()) {
            throw new IllegalArgumentException("Request body is required");
        }

        Object sourceConfigObj = requestPayload.get("source_config");
        if (!(sourceConfigObj instanceof Map<?, ?> sourceConfig)) {
            throw new IllegalArgumentException("source_config is required and must be an object");
        }

        Object urlObj = sourceConfig.get("url");
        if (!(urlObj instanceof String url) || url.isBlank()) {
            throw new IllegalArgumentException("source_config.url is required");
        }
    }

    private ApiKey requireApiKey(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Missing or invalid Authorization header. Expected 'Bearer <API_KEY>'");
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        String hashedToken = apiKeyService.hashKey(token);

        ApiKey apiKey = apiKeyRepository.findByKey(hashedToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid API Key"));

        if (apiKey.getStatus() != ApiKeyStatus.ACTIVE) {
            throw new IllegalArgumentException("API Key is not active");
        }

        return apiKey;
    }
}