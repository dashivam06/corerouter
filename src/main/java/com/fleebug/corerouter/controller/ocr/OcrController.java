package com.fleebug.corerouter.controller.ocr;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.entity.apikey.ApiKey;
import com.fleebug.corerouter.enums.apikey.ApiKeyStatus;
import com.fleebug.corerouter.repository.apikey.ApiKeyRepository;
import com.fleebug.corerouter.service.apikey.ApiKeyService;
import com.fleebug.corerouter.service.ocr.OcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ocr")
@RequiredArgsConstructor
@Tag(name = "OCR", description = "OCR endpoints")
public class OcrController {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final OcrService ocrService;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyService apiKeyService;
    private final TelemetryClient telemetryClient;

    @Operation(
            summary = "Parse text from image URL",
            description = "Fetch OCR results from ocr.space for the provided image URL. Requires 'Authorization: Bearer <API_KEY>' header."
    )
    @PostMapping("/parse/imageurl")
    public ResponseEntity<ApiResponse<Map<String, Object>>> parseImageUrl(
            @RequestParam("url") String imageUrl,
            HttpServletRequest request) {

        Map<String, String> properties = new HashMap<>();
        properties.put("provider", "ocr.space");
        telemetryClient.trackTrace("OCR parse image URL request", SeverityLevel.Information, properties);

        requireApiKey(request);

        Map<String, Object> ocrResponse = ocrService.parseImageUrl(imageUrl);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "OCR parsed successfully", ocrResponse, request));
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