package com.fleebug.corerouter.controller.model;

import com.fleebug.corerouter.dto.common.ApiResponse;
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

    /**
     * Get all active models (For users)
     * Returns only ACTIVE models that users can interact with
     * 
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse containing list of active models
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ModelResponse>>> getActiveModels(HttpServletRequest request) {
        try {
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
        } catch (Exception e) {
            log.error("Unexpected error while fetching active models", e);
            ApiResponse<List<ModelResponse>> response = ApiResponse.<List<ModelResponse>>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .success(false)
                    .message("An unexpected error occurred")
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get active model by ID (For users)
     * Returns only if the model is ACTIVE
     * 
     * @param modelId Model ID
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse containing model details
     */
    @GetMapping("/{modelId}")
    public ResponseEntity<ApiResponse<ModelResponse>> getModelById(
            @PathVariable Integer modelId,
            HttpServletRequest request) {
        try {
            log.info("User requesting model details for ID: {}", modelId);
            ModelResponse model = modelService.getModelById(modelId);
            
            // Check if model is ACTIVE
            if (!"ACTIVE".equals(model.getStatus().toString())) {
                log.warn("User attempted to access inactive model ID: {}", modelId);
                ApiResponse<ModelResponse> response = ApiResponse.<ModelResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.NOT_FOUND.value())
                        .success(false)
                        .message("Model not found or is not available")
                        .path(request.getRequestURI())
                        .method(request.getMethod())
                        .data(null)
                        .build();
                
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            ApiResponse<ModelResponse> response = ApiResponse.<ModelResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.OK.value())
                    .success(true)
                    .message("Model retrieved successfully")
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(model)
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Model not found: {}", e.getMessage());
            ApiResponse<ModelResponse> response = ApiResponse.<ModelResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.NOT_FOUND.value())
                    .success(false)
                    .message("Model not found")
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Unexpected error while fetching model", e);
            ApiResponse<ModelResponse> response = ApiResponse.<ModelResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .success(false)
                    .message("An unexpected error occurred")
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
