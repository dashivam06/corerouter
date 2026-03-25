package com.fleebug.corerouter.controller.model;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/models")
@RequiredArgsConstructor
@Tag(name = "Admin Models", description = "Model management for administrators — CRUD, status changes, archival, and audit history")
public class ModelController {

    private final ModelService modelService;
    private final TelemetryClient telemetryClient;

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
        
        Map<String, String> properties = new HashMap<>();
        properties.put("modelName", createRequest.getFullname());
        telemetryClient.trackTrace("Create model request", SeverityLevel.Verbose, properties);

        User admin = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        
        ModelResponse response = modelService.createModel(createRequest, admin);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Model created successfully", response, request));
    }

    @Operation(summary = "Get all models", description = "Retrieve all models regardless of status")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ModelResponse>>> getAllModels(HttpServletRequest request) {
        
        telemetryClient.trackTrace("Get all models request", SeverityLevel.Verbose, null);

        List<ModelResponse> models = modelService.getAllModels();
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Models retrieved successfully", models, request));
    }

    @Operation(summary = "Get models by status", description = "Retrieve models filtered by their current status")
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<ModelResponse>>> getModelsByStatus(
            @Parameter(description = "Model status filter", example = "ACTIVE") @PathVariable ModelStatus status,
            HttpServletRequest request) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("status", status.name());
        telemetryClient.trackTrace("Get models by status request", SeverityLevel.Verbose, properties);

        List<ModelResponse> models = modelService.getModelsByStatus(status);
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Models retrieved successfully", models, request));
    }

    @Operation(summary = "Get model by ID", description = "Retrieve a single model by its ID")
    @GetMapping("/{modelId}")
    public ResponseEntity<ApiResponse<ModelResponse>> getModelById(
            @Parameter(description = "Model ID", example = "1") @PathVariable Integer modelId,
            HttpServletRequest request) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("modelId", String.valueOf(modelId));
        telemetryClient.trackTrace("Get model by ID request", SeverityLevel.Verbose, properties);

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
        
        Map<String, String> properties = new HashMap<>();
        properties.put("modelId", String.valueOf(modelId));
        telemetryClient.trackTrace("Update model request", SeverityLevel.Verbose, properties);

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
        
        Map<String, String> properties = new HashMap<>();
        properties.put("modelId", String.valueOf(modelId));
        properties.put("newStatus", String.valueOf(statusRequest.getStatus()));
        telemetryClient.trackEvent("ModelStatusUpdate", properties, null);

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
        
        Map<String, String> properties = new HashMap<>();
        properties.put("modelId", String.valueOf(modelId));
        telemetryClient.trackEvent("ModelDeletion", properties, null);

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
        
        Map<String, String> properties = new HashMap<>();
        properties.put("modelId", String.valueOf(modelId));
        telemetryClient.trackTrace("Archive model request", SeverityLevel.Verbose, properties);

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
        
        Map<String, String> properties = new HashMap<>();
        properties.put("modelId", String.valueOf(modelId));
        telemetryClient.trackTrace("Inactivate model request", SeverityLevel.Verbose, properties);

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
        
        Map<String, String> properties = new HashMap<>();
        properties.put("modelId", String.valueOf(modelId));
        telemetryClient.trackTrace("Get audit history request", SeverityLevel.Verbose, properties);

        List<ModelStatusAudit> auditRecords = modelService.getModelAuditHistory(modelId);
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Audit history retrieved successfully", auditRecords, request));
    }
}