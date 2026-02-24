package com.fleebug.corerouter.controller.model;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.model.response.ModelDetailsResponse;
import com.fleebug.corerouter.dto.model.response.ModelResponse;
import com.fleebug.corerouter.service.model.ModelService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User-facing Model API endpoints
 * Allows authenticated users to view active models
 */
@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
@Slf4j
public class UserModelController {

    private final ModelService modelService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ModelResponse>>> getActiveModels(HttpServletRequest request) {
        log.info("User requesting active models list");
        List<ModelResponse> models = modelService.getActiveModels();
        
        ApiResponse<List<ModelResponse>> response = ApiResponse.<List<ModelResponse>>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .success(true)
                .message("Active models retrieved successfully")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(models)
                .build();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{modelId}")
    public ResponseEntity<ApiResponse<ModelDetailsResponse>> getModelById(
            @PathVariable Integer modelId,
            HttpServletRequest request) {
        log.info("User requesting model details for ID: {}", modelId);
        ModelDetailsResponse model = modelService.getModelDetailsWithDocumentation(modelId);
        
        // Check if model is ACTIVE
        if (!"ACTIVE".equals(model.getStatus().toString())) {
            log.warn("User attempted to access inactive model ID: {}", modelId);
            throw new IllegalArgumentException("Model not found or is not available");
        }
        
        ApiResponse<ModelDetailsResponse> response = ApiResponse.<ModelDetailsResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .success(true)
                .message("Model and documentation retrieved successfully")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(model)
                .build();
        
        return ResponseEntity.ok(response);
    }
}
