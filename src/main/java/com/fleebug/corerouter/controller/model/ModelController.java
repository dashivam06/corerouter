package com.fleebug.corerouter.controller.model;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.model.request.CreateModelRequest;
import com.fleebug.corerouter.dto.model.request.UpdateModelRequest;
import com.fleebug.corerouter.dto.model.request.UpdateModelStatusRequest;
import com.fleebug.corerouter.dto.model.response.ModelResponse;
import com.fleebug.corerouter.entity.model.ModelStatusAudit;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.model.ModelStatus;
import com.fleebug.corerouter.security.details.CustomUserDetails;
import com.fleebug.corerouter.service.model.ModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/models")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Models", description = "Model management for administrators — CRUD, status changes, archival, and audit history")
public class ModelController {

    private final ModelService modelService;

    @Operation(summary = "Create model", description = "Register a new AI model in the system")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Model created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Model already exists")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModelResponse>> createModel(
            @Valid @RequestBody CreateModelRequest createRequest,
            Authentication authentication,
            HttpServletRequest request) {
        log.debug("Create model request received: {}", createRequest.getFullname());
        User admin = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        
        ModelResponse response = modelService.createModel(createRequest, admin);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Model created successfully", response, request));
    }

    @Operation(summary = "Get all models", description = "Retrieve all models regardless of status")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ModelResponse>>> getAllModels(HttpServletRequest request) {
        log.debug("Get all models request received");
        List<ModelResponse> models = modelService.getAllModels();
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Models retrieved successfully", models, request));
    }

    @Operation(summary = "Get models by status", description = "Retrieve models filtered by their current status")
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<ModelResponse>>> getModelsByStatus(
            @Parameter(description = "Model status filter", example = "ACTIVE") @PathVariable ModelStatus status,
            HttpServletRequest request) {
        log.debug("Get models by status request received: {}", status);
        List<ModelResponse> models = modelService.getModelsByStatus(status);
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Models retrieved successfully", models, request));
    }

    @Operation(summary = "Get model by ID", description = "Retrieve a single model by its ID")
    @GetMapping("/{modelId}")
    public ResponseEntity<ApiResponse<ModelResponse>> getModelById(
            @Parameter(description = "Model ID", example = "1") @PathVariable Integer modelId,
            HttpServletRequest request) {
        log.debug("Get model by ID request received: {}", modelId);
        ModelResponse model = modelService.getModelById(modelId);
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Model retrieved successfully", model, request));
    }

    @Operation(summary = "Update model", description = "Update an existing model's details")
    @PutMapping("/{modelId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModelResponse>> updateModel(
            @Parameter(description = "Model ID", example = "1") @PathVariable Integer modelId,
            @Valid @RequestBody UpdateModelRequest updateRequest,
            Authentication authentication,
            HttpServletRequest request) {
        log.debug("Update model request received for ID: {}", modelId);
        User admin = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        
        ModelResponse response = modelService.updateModel(modelId, updateRequest, admin);
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Model updated successfully", response, request));
    }

    @Operation(summary = "Update model status", description = "Change a model's status (e.g. ACTIVE, INACTIVE, ARCHIVED)")
    @PatchMapping("/{modelId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModelResponse>> updateModelStatus(
            @Parameter(description = "Model ID", example = "1") @PathVariable Integer modelId,
            @Valid @RequestBody UpdateModelStatusRequest statusRequest,
            Authentication authentication,
            HttpServletRequest request) {
        log.debug("Update model status request received for ID: {} to {}", modelId, statusRequest.getStatus());
        User admin = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        
        ModelResponse response = modelService.changeModelStatus(modelId, statusRequest.getStatus(), admin);
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Model status updated successfully", response, request));
    }

    @Operation(summary = "Delete model", description = "Permanently delete a model")
    @DeleteMapping("/{modelId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteModel(
            @Parameter(description = "Model ID", example = "1") @PathVariable Integer modelId,
            Authentication authentication,
            HttpServletRequest request) {
        log.debug("Delete model request received for ID: {}", modelId);
        User admin = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        
        modelService.deleteModel(modelId, admin);
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Model deleted permanently", null, request));
    }

    @Operation(summary = "Archive model", description = "Soft-archive a model so it is no longer active")
    @PostMapping("/{modelId}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> archiveModel(
            @Parameter(description = "Model ID", example = "1") @PathVariable Integer modelId,
            Authentication authentication,
            HttpServletRequest request) {
        log.debug("Archive model request received for ID: {}", modelId);
        User admin = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        
        modelService.archiveModel(modelId, admin);
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Model archived successfully", null, request));
    }

    @Operation(summary = "Inactivate model", description = "Mark a model as inactive")
    @PostMapping("/{modelId}/inactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModelResponse>> inactivateModel(
            @Parameter(description = "Model ID", example = "1") @PathVariable Integer modelId,
            Authentication authentication,
            HttpServletRequest request) {
        log.info("Inactivate model request received for ID: {}", modelId);
        User admin = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        
        ModelResponse response = modelService.inactivateModel(modelId, admin);
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Model inactivated successfully", response, request));
    }

    @Operation(summary = "Get audit history", description = "Retrieve the status-change audit trail for a model")
    @GetMapping("/{modelId}/audit-history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ModelStatusAudit>>> getAuditHistory(
            @Parameter(description = "Model ID", example = "1") @PathVariable Integer modelId,
            HttpServletRequest request) {
        log.info("Get audit history request received for model ID: {}", modelId);
        List<ModelStatusAudit> auditRecords = modelService.getModelAuditHistory(modelId);
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Audit history retrieved successfully", auditRecords, request));
    }
}