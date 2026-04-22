package com.fleebug.corerouter.service.speech;

import com.fleebug.corerouter.util.HttpClientUtil;
import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SpeechToTextService {

    private static final String REV_AI_JOBS_URL = "https://api.rev.ai/speechtotext/v1/jobs";
    private static final String REV_TRANSCRIPT_ACCEPT = "application/vnd.rev.transcript.v1.0+json";

    private final HttpClientUtil httpClientUtil;

    @Value("${rev.ai.access-token:02vVaTNM6iehZUsKcthtuT0GsQ9GybTfzK23HPloWoCxbcLO8Z-EDwPcZCvcSIWjGwtupWLxcdHwZ2pmyEgKdZEvGXIpE}")
    private String revAiAccessToken;

    public Map<String, Object> createJob(Map<String, Object> requestPayload) {
        if (requestPayload == null || requestPayload.isEmpty()) {
            throw new IllegalArgumentException("Request body is required");
        }

        String token = resolveRevAiToken();

        try {
            Object rawResponse = httpClientUtil.postJsonForObject(
                    REV_AI_JOBS_URL,
                    Map.of(
                            "Authorization", "Bearer " + token,
                            "Content-Type", "application/json"
                    ),
                    requestPayload,
                    Object.class,
                    5000,
                    30000
            );

            return toTypedMap(rawResponse);
        } catch (IllegalStateException ex) {
            throw mapProviderException(ex);
        }
    }

    public Map<String, Object> getJob(String jobId) {
        validateJobId(jobId);
        String token = resolveRevAiToken();

        try {
            return httpClientUtil.getJsonMap(
                    buildJobUrl(jobId, false),
                    Map.of("Authorization", "Bearer " + token),
                    5000,
                    30000
            );
        } catch (IllegalStateException ex) {
            throw mapProviderException(ex);
        }
    }

    public Map<String, Object> getTranscript(String jobId) {
        validateJobId(jobId);
        String token = resolveRevAiToken();

        try {
            return httpClientUtil.getJsonMap(
                    buildJobUrl(jobId, true),
                    Map.of(
                            "Authorization", "Bearer " + token,
                            "Accept", REV_TRANSCRIPT_ACCEPT
                    ),
                    5000,
                    30000
            );
        } catch (IllegalStateException ex) {
            throw mapProviderException(ex);
        }
    }

    public Map<String, Object> getTranscriptText(String jobId) {
        Map<String, Object> transcript = getTranscript(jobId);
        String text = extractText(transcript);

        return Map.of(
                "jobId", jobId,
                "text", text
        );
    }

    private String resolveRevAiToken() {
        String token = revAiAccessToken;

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Rev AI access token is missing in backend configuration.");
        }

        token = token.trim();
        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }

        if (token.isBlank()) {
            throw new IllegalArgumentException("Rev AI access token is blank after trimming.");
        }

        return token;
    }

    private IllegalArgumentException mapProviderException(IllegalStateException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage();

        if (message.contains("status 401")) {
            return new IllegalArgumentException(
                    "Speech-to-text provider authorization failed (401). Update backend 'rev.ai.access-token' with a valid key.",
                    ex
            );
        }

        return new IllegalArgumentException("Speech-to-text provider request failed: " + message, ex);
    }

    private void validateJobId(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("jobId is required");
        }
    }

    private String buildJobUrl(String jobId, boolean transcript) {
        HttpUrl.Builder builder = new HttpUrl.Builder()
            .scheme("https")
            .host("api.rev.ai")
            .addPathSegment("speechtotext")
            .addPathSegment("v1")
            .addPathSegment("jobs")
            .addPathSegment(jobId);

        if (transcript) {
            builder.addPathSegment("transcript");
        }

        return builder.build().toString();
    }

    private String extractText(Map<String, Object> transcript) {
        Object monologuesObj = transcript.get("monologues");
        if (!(monologuesObj instanceof List<?> monologues)) {
            return "";
        }

        List<String> tokens = new ArrayList<>();
        for (Object monologueObj : monologues) {
            if (!(monologueObj instanceof Map<?, ?> monologue)) {
                continue;
            }

            Object elementsObj = monologue.get("elements");
            if (!(elementsObj instanceof List<?> elements)) {
                continue;
            }

            for (Object elementObj : elements) {
                if (!(elementObj instanceof Map<?, ?> element)) {
                    continue;
                }

                Object valueObj = element.get("value");
                if (valueObj instanceof String value) {
                    tokens.add(value);
                }
            }
        }

        return String.join("", tokens).trim();
    }

    private Map<String, Object> toTypedMap(Object rawResponse) {
        if (!(rawResponse instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("Unexpected provider response format");
        }

        Map<String, Object> typed = new LinkedHashMap<>();
        rawMap.forEach((key, value) -> typed.put(String.valueOf(key), value));
        return typed;
    }
}