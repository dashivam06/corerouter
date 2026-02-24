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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModelResponse>> createModel(
            @Valid @RequestBody CreateModelRequest createRequest,
            Authentication authentication,
            HttpServletRequest request) {
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
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ModelResponse>>> getAllModels(HttpServletRequest request) {
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
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<ModelResponse>>> getModelsByStatus(
            @PathVariable ModelStatus status,
            HttpServletRequest request) {
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
    }

    @GetMapping("/{modelId}")
    public ResponseEntity<ApiResponse<ModelResponse>> getModelById(
            @PathVariable Integer modelId,
            HttpServletRequest request) {
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
    }

    @PutMapping("/{modelId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModelResponse>> updateModel(
            @PathVariable Integer modelId,
            @Valid @RequestBody UpdateModelRequest updateRequest,
            Authentication authentication,
            HttpServletRequest request) {
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
    }

    @PatchMapping("/{modelId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModelResponse>> updateModelStatus(
            @PathVariable Integer modelId,
            @Valid @RequestBody UpdateModelStatusRequest statusRequest,
            Authentication authentication,
            HttpServletRequest request) {
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
    }

    @DeleteMapping("/{modelId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteModel(
            @PathVariable Integer modelId,
            Authentication authentication,
            HttpServletRequest request) {
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
    }

    @PostMapping("/{modelId}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> archiveModel(
            @PathVariable Integer modelId,
            Authentication authentication,
            HttpServletRequest request) {
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
    }

    @PostMapping("/{modelId}/inactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModelResponse>> inactivateModel(
            @PathVariable Integer modelId,
            Authentication authentication,
            HttpServletRequest request) {
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
    }

    @GetMapping("/{modelId}/audit-history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ModelStatusAudit>>> getAuditHistory(
            @PathVariable Integer modelId,
            HttpServletRequest request) {
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
    }
}