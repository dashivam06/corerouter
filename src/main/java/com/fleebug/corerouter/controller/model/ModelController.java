package com.fleebug.corerouter.controller.model;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.model.request.CreateModelRequest;
import com.fleebug.corerouter.dto.model.request.UpdateModelRequest;
import com.fleebug.corerouter.dto.model.request.UpdateModelStatusRequest;
import com.fleebug.corerouter.dto.model.response.ModelResponse;
import com.fleebug.corerouter.enums.model.ModelStatus;
import com.fleebug.corerouter.model.model.ModelStatusAudit;
import com.fleebug.corerouter.model.user.User;
import com.fleebug.corerouter.security.details.CustomUserDetails;
import com.fleebug.corerouter.service.model.ModelService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/models")
@RequiredArgsConstructor
@Slf4j
public class ModelController {

    private final ModelService modelService;

    /**
     * Create a new model (Admin only)
     * 
     * @param createRequest Create model request
     * @param authentication Authenticated admin user
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse containing created model
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModelResponse>> createModel(
            @Valid @RequestBody CreateModelRequest createRequest,
            Authentication authentication,
            HttpServletRequest request) {
        try {
            log.info("Create model request received: {}", createRequest.getFullname());
            User admin = ((CustomUserDetails) authentication.getPrincipal()).getUser();
            
            ModelResponse response = modelService.createModel(createRequest, admin);
            
            ApiResponse<ModelResponse> apiResponse = ApiResponse.<ModelResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.CREATED.value())
                    .success(true)
                    .message("Model created successfully")
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(response)
                    .build();
            
            return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
        } catch (IllegalArgumentException e) {
            log.error("Create model error: {}", e.getMessage());
            ApiResponse<ModelResponse> response = ApiResponse.<ModelResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .success(false)
                    .message(e.getMessage())
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Unexpected error during model creation", e);
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

    /**
     * Get all models (Accessible to all authenticated users)
     * 
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse containing list of models
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ModelResponse>>> getAllModels(HttpServletRequest request) {
        try {
            log.info("Get all models request received");
            List<ModelResponse> models = modelService.getAllModels();
            
            ApiResponse<List<ModelResponse>> response = ApiResponse.<List<ModelResponse>>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.OK.value())
                    .success(true)
                    .message("Models retrieved successfully")
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(models)
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Unexpected error while fetching models", e);
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
     * Get models by status
     * 
     * @param status Model status
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse containing filtered models
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<ModelResponse>>> getModelsByStatus(
            @PathVariable ModelStatus status,
            HttpServletRequest request) {
        try {
            log.info("Get models by status request received: {}", status);
            List<ModelResponse> models = modelService.getModelsByStatus(status);
            
            ApiResponse<List<ModelResponse>> response = ApiResponse.<List<ModelResponse>>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.OK.value())
                    .success(true)
                    .message("Models retrieved successfully")
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(models)
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Unexpected error while fetching models by status", e);
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
     * Get model by ID
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
            log.info("Get model by ID request received: {}", modelId);
            ModelResponse model = modelService.getModelById(modelId);
            
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
            log.error("Get model error: {}", e.getMessage());
            ApiResponse<ModelResponse> response = ApiResponse.<ModelResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.NOT_FOUND.value())
                    .success(false)
                    .message(e.getMessage())
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

    /**
     * Update model details (Admin only)
     * 
     * @param modelId Model ID
     * @param updateRequest Update model request
     * @param authentication Authenticated admin user
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse containing updated model
     */
    @PutMapping("/{modelId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModelResponse>> updateModel(
            @PathVariable Integer modelId,
            @Valid @RequestBody UpdateModelRequest updateRequest,
            Authentication authentication,
            HttpServletRequest request) {
        try {
            log.info("Update model request received for ID: {}", modelId);
            User admin = ((CustomUserDetails) authentication.getPrincipal()).getUser();
            
            ModelResponse response = modelService.updateModel(modelId, updateRequest, admin);
            
            ApiResponse<ModelResponse> apiResponse = ApiResponse.<ModelResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.OK.value())
                    .success(true)
                    .message("Model updated successfully")
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(response)
                    .build();
            
            return ResponseEntity.ok(apiResponse);
        } catch (IllegalArgumentException e) {
            log.error("Update model error: {}", e.getMessage());
            ApiResponse<ModelResponse> response = ApiResponse.<ModelResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .success(false)
                    .message(e.getMessage())
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Unexpected error during model update", e);
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

    /**
     * Update model status (Admin only)
     * Records status change in ModelStatusAudit table
     * 
     * @param modelId Model ID
     * @param statusRequest Status update request
     * @param authentication Authenticated admin user
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse containing updated model
     */
    @PatchMapping("/{modelId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModelResponse>> updateModelStatus(
            @PathVariable Integer modelId,
            @Valid @RequestBody UpdateModelStatusRequest statusRequest,
            Authentication authentication,
            HttpServletRequest request) {
        try {
            log.info("Update model status request received for ID: {} to {}", modelId, statusRequest.getStatus());
            User admin = ((CustomUserDetails) authentication.getPrincipal()).getUser();
            
            ModelResponse response = modelService.changeModelStatus(modelId, statusRequest.getStatus(), admin);
            
            ApiResponse<ModelResponse> apiResponse = ApiResponse.<ModelResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.OK.value())
                    .success(true)
                    .message("Model status updated successfully")
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(response)
                    .build();
            
            return ResponseEntity.ok(apiResponse);
        } catch (IllegalArgumentException e) {
            log.error("Update model status error: {}", e.getMessage());
            ApiResponse<ModelResponse> response = ApiResponse.<ModelResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .success(false)
                    .message(e.getMessage())
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Unexpected error during model status update", e);
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

    /**
     * Delete model (Admin only) - Hard delete
     * 
     * @param modelId Model ID
     * @param authentication Authenticated admin user
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse
     */
    @DeleteMapping("/{modelId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteModel(
            @PathVariable Integer modelId,
            Authentication authentication,
            HttpServletRequest request) {
        try {
            log.info("Delete model request received for ID: {}", modelId);
            User admin = ((CustomUserDetails) authentication.getPrincipal()).getUser();
            
            modelService.deleteModel(modelId, admin);
            
            ApiResponse<Void> response = ApiResponse.<Void>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.OK.value())
                    .success(true)
                    .message("Model deleted permanently")
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Delete model error: {}", e.getMessage());
            ApiResponse<Void> response = ApiResponse.<Void>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.NOT_FOUND.value())
                    .success(false)
                    .message(e.getMessage())
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Unexpected error during model deletion", e);
            ApiResponse<Void> response = ApiResponse.<Void>builder()
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
     * Archive model (Admin only) - Soft delete
     * 
     * @param modelId Model ID
     * @param authentication Authenticated admin user
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse
     */
    @PostMapping("/{modelId}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> archiveModel(
            @PathVariable Integer modelId,
            Authentication authentication,
            HttpServletRequest request) {
        try {
            log.info("Archive model request received for ID: {}", modelId);
            User admin = ((CustomUserDetails) authentication.getPrincipal()).getUser();
            
            modelService.archiveModel(modelId, admin);
            
            ApiResponse<Void> response = ApiResponse.<Void>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.OK.value())
                    .success(true)
                    .message("Model archived successfully")
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Archive model error: {}", e.getMessage());
            ApiResponse<Void> response = ApiResponse.<Void>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.NOT_FOUND.value())
                    .success(false)
                    .message(e.getMessage())
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Unexpected error during model archiving", e);
            ApiResponse<Void> response = ApiResponse.<Void>builder()
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
     * Inactivate model (Admin only)
     * 
     * @param modelId Model ID
     * @param authentication Authenticated admin user
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse
     */
    @PostMapping("/{modelId}/inactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModelResponse>> inactivateModel(
            @PathVariable Integer modelId,
            Authentication authentication,
            HttpServletRequest request) {
        try {
            log.info("Inactivate model request received for ID: {}", modelId);
            User admin = ((CustomUserDetails) authentication.getPrincipal()).getUser();
            
            ModelResponse response = modelService.inactivateModel(modelId, admin);
            
            ApiResponse<ModelResponse> apiResponse = ApiResponse.<ModelResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.OK.value())
                    .success(true)
                    .message("Model inactivated successfully")
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(response)
                    .build();
            
            return ResponseEntity.ok(apiResponse);
        } catch (IllegalArgumentException e) {
            log.error("Inactivate model error: {}", e.getMessage());
            ApiResponse<ModelResponse> response = ApiResponse.<ModelResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .success(false)
                    .message(e.getMessage())
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Unexpected error during model inactivation", e);
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

    /**
     * Get model status audit history (Admin only)
     * 
     * @param modelId Model ID
     * @param request HttpServletRequest for response metadata
     * @return ResponseEntity with ApiResponse containing audit records
     */
    @GetMapping("/{modelId}/audit-history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ModelStatusAudit>>> getAuditHistory(
            @PathVariable Integer modelId,
            HttpServletRequest request) {
        try {
            log.info("Get audit history request received for model ID: {}", modelId);
            List<ModelStatusAudit> auditRecords = modelService.getModelAuditHistory(modelId);
            
            ApiResponse<List<ModelStatusAudit>> response = ApiResponse.<List<ModelStatusAudit>>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.OK.value())
                    .success(true)
                    .message("Audit history retrieved successfully")
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(auditRecords)
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Get audit history error: {}", e.getMessage());
            ApiResponse<List<ModelStatusAudit>> response = ApiResponse.<List<ModelStatusAudit>>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.NOT_FOUND.value())
                    .success(false)
                    .message(e.getMessage())
                    .path(request.getRequestURI())
                    .method(request.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Unexpected error while fetching audit history", e);
            ApiResponse<List<ModelStatusAudit>> response = ApiResponse.<List<ModelStatusAudit>>builder()
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
