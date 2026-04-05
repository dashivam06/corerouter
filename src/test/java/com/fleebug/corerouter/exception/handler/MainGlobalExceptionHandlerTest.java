package com.fleebug.corerouter.exception.handler;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.exception.model.ModelNotFoundException;
import com.microsoft.applicationinsights.TelemetryClient;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class MainGlobalExceptionHandlerTest {

    private final TelemetryClient telemetryClient = mock(TelemetryClient.class);
    private final MainGlobalExceptionHandler handler = new MainGlobalExceptionHandler(telemetryClient);

    @Test
    void handleModelNotFoundExceptionReturnsNotFoundResponse() {
        HttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/chat/completions");
        ModelNotFoundException exception = new ModelNotFoundException("fullname", "Qwen/Qwen2-0.5B-Instruct");

        var response = handler.handleModelNotFoundException(exception, request);
        ApiResponse<Void> body = response.getBody();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(body.isSuccess());
        assertEquals(404, body.getStatus());
        assertEquals("Model with fullname 'Qwen/Qwen2-0.5B-Instruct' not found", body.getMessage());
        assertEquals("/api/v1/chat/completions", body.getPath());
        assertEquals("POST", body.getMethod());
    }
}