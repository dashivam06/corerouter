package com.fleebug.corerouter.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class HttpClientUtil {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final ObjectMapper objectMapper;

    public String get(String url) {
        return get(url, Map.of(), 5000, 5000);
    }

    public String get(String url, Map<String, String> headers, int connectTimeoutMs, int readTimeoutMs) {
        Request.Builder requestBuilder = new Request.Builder().url(url).get();
        applyHeaders(requestBuilder, headers);

        try (Response response = client(connectTimeoutMs, readTimeoutMs)
                .newCall(requestBuilder.build())
                .execute()) {
            return handleResponse(response, url);
        } catch (IOException ex) {
            throw new IllegalStateException("HTTP GET failed for " + url + ": " + ex.getMessage(), ex);
        }
    }

    public <T> T getJson(String url, Map<String, String> headers, Class<T> responseType, int connectTimeoutMs, int readTimeoutMs) {
        String responseBody = get(url, headers, connectTimeoutMs, readTimeoutMs);
        try {
            return objectMapper.readValue(responseBody, responseType);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse JSON response from " + url + ": " + ex.getMessage(), ex);
        }
    }

    public Map<String, Object> getJsonMap(String url, Map<String, String> headers, int connectTimeoutMs, int readTimeoutMs) {
        String responseBody = get(url, headers, connectTimeoutMs, readTimeoutMs);
        try {
            return objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse JSON response from " + url + ": " + ex.getMessage(), ex);
        }
    }

    public String postJson(String url, Map<String, String> headers, Object payload, int connectTimeoutMs, int readTimeoutMs) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            RequestBody requestBody = RequestBody.create(payloadJson, JSON);

            Request.Builder requestBuilder = new Request.Builder().url(url).post(requestBody);
            applyHeaders(requestBuilder, headers);

            try (Response response = client(connectTimeoutMs, readTimeoutMs)
                    .newCall(requestBuilder.build())
                    .execute()) {
                return handleResponse(response, url);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("HTTP POST failed for " + url + ": " + ex.getMessage(), ex);
        }
    }

    public <T> T postJsonForObject(String url, Map<String, String> headers, Object payload, Class<T> responseType,
                                   int connectTimeoutMs, int readTimeoutMs) {
        String responseBody = postJson(url, headers, payload, connectTimeoutMs, readTimeoutMs);
        try {
            return objectMapper.readValue(responseBody, responseType);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse JSON response from " + url + ": " + ex.getMessage(), ex);
        }
    }

    public Map<String, Object> postFormForMap(String url,
                                              Map<String, String> headers,
                                              Map<String, String> formData,
                                              int connectTimeoutMs,
                                              int readTimeoutMs) {
        try {
            FormBody.Builder formBuilder = new FormBody.Builder();
            if (formData != null) {
                formData.forEach((key, value) -> formBuilder.add(key, value == null ? "" : value));
            }

            Request.Builder requestBuilder = new Request.Builder()
                    .url(HttpUrl.get(url))
                    .post(formBuilder.build());
            applyHeaders(requestBuilder, headers);

            try (Response response = client(connectTimeoutMs, readTimeoutMs)
                    .newCall(requestBuilder.build())
                    .execute()) {
                String responseBody = handleResponse(response, url);
                return objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            }
        } catch (IOException ex) {
            throw new IllegalStateException("HTTP POST form failed for " + url + ": " + ex.getMessage(), ex);
        }
    }

    private OkHttpClient client(int connectTimeoutMs, int readTimeoutMs) {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .readTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }

    private void applyHeaders(Request.Builder builder, Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        headers.forEach(builder::addHeader);
    }

    private String handleResponse(Response response, String url) throws IOException {
        String body = response.body() == null ? "" : response.body().string();
        if (!response.isSuccessful()) {
            throw new IllegalStateException("HTTP call failed for " + url + " with status " + response.code() + " and body: " + body);
        }
        return body;
    }
}
