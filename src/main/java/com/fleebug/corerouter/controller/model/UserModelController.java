package com.fleebug.corerouter.controller.model;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.model.response.ModelDetailsResponse;
import com.fleebug.corerouter.dto.model.response.ModelResponse;
import com.fleebug.corerouter.service.model.ModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User-facing Model API endpoints
 * Allows authenticated users to view active models
 */
@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
@Tag(name = "Models", description = "User-facing endpoints to browse active AI models")
public class UserModelController {

    private final ModelService modelService;
    private final TelemetryClient telemetryClient;

    @Operation(summary = "List active models", description = "Retrieve all models that are currently active")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ModelResponse>>> getActiveModels(HttpServletRequest request) {
        telemetryClient.trackTrace("User requesting active models list", SeverityLevel.Verbose, null);
        List<ModelResponse> models = modelService.getActiveModels();
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Active models retrieved successfully", models, request));
    }

    @Operation(summary = "Get model details", description = "Retrieve model details including documentation for an active model")
    @GetMapping("/{modelId}")
    public ResponseEntity<ApiResponse<ModelDetailsResponse>> getModelById(
            @Parameter(description = "Model ID", example = "1") @PathVariable Integer modelId,
            HttpServletRequest request) {
        Map<String, String> properties = new HashMap<>();
        properties.put("modelId", String.valueOf(modelId));
        telemetryClient.trackTrace("User requesting model details", SeverityLevel.Verbose, properties);

        ModelDetailsResponse model = modelService.getModelDetailsWithDocumentation(modelId);
        
        // Check if model is ACTIVE
        if (!"ACTIVE".equals(model.getStatus().toString())) {
            telemetryClient.trackTrace("User attempted to access inactive model", SeverityLevel.Warning, properties);
            throw new IllegalArgumentException("Model not found or is not available");
        }
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Model and documentation retrieved successfully", model, request));
    }
}
