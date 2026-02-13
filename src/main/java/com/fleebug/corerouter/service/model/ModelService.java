package com.fleebug.corerouter.service.model;

import com.fleebug.corerouter.dto.model.request.CreateModelRequest;
import com.fleebug.corerouter.dto.model.request.UpdateModelRequest;
import com.fleebug.corerouter.dto.model.response.ModelResponse;
import com.fleebug.corerouter.enums.model.ModelStatus;
import com.fleebug.corerouter.model.model.Model;
import com.fleebug.corerouter.model.model.ModelStatusAudit;
import com.fleebug.corerouter.model.user.User;
import com.fleebug.corerouter.repository.model.ModelRepository;
import com.fleebug.corerouter.repository.model.ModelStatusAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ModelService {

    private final ModelRepository modelRepository;
    private final ModelStatusAuditRepository modelStatusAuditRepository;

    /**
     * Create a new model (Admin only)
     * 
     * @param createRequest Create model request
     * @param admin Admin user performing the action
     * @return Created model response
     */
    public ModelResponse createModel(CreateModelRequest createRequest, User admin) {
        log.info("Creating new model: {} by admin: {}", createRequest.getFullname(), admin.getUserId());

        if (modelRepository.existsByFullname(createRequest.getFullname())) {
            log.warn("Model with name already exists: {}", createRequest.getFullname());
            throw new IllegalArgumentException("Model with this name already exists");
        }

        Model model = Model.builder()
                .fullname(createRequest.getFullname())
                .username(createRequest.getUsername())
                .provider(createRequest.getProvider())
                .parameterCount(createRequest.getParameterCount())
                .pricePer1kTokens(createRequest.getPricePer1kTokens())
                .status(ModelStatus.ACTIVE)
                .endpointUrl(createRequest.getEndpointUrl())
                .type(createRequest.getType())
                .createdAt(LocalDateTime.now())
                .build();

        Model savedModel = modelRepository.save(model);
        log.info("Model created successfully with ID: {}", savedModel.getModelId());

        // Record status audit for new model creation
        createAuditLog(savedModel, ModelStatus.NOTHING, ModelStatus.ACTIVE, admin, "Model created");

        return mapToResponse(savedModel);
    }

    /**
     * Get all models
     * 
     * @return List of model responses
     */
    @Transactional(readOnly = true)
    public List<ModelResponse> getAllModels() {
        log.info("Fetching all models");
        return modelRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get models by status
     * 
     * @param status Model status
     * @return List of model responses
     */
    @Transactional(readOnly = true)
    public List<ModelResponse> getModelsByStatus(ModelStatus status) {
        log.info("Fetching models with status: {}", status);
        return modelRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get model by ID
     * 
     * @param modelId Model ID
     * @return Model response
     */
    @Transactional(readOnly = true)
    public ModelResponse getModelById(Integer modelId) {
        log.info("Fetching model with ID: {}", modelId);
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> {
                    log.warn("Model not found with ID: {}", modelId);
                    return new IllegalArgumentException("Model not found");
                });

        return mapToResponse(model);
    }

    /**
     * Update model details (Admin only)
     * 
     * @param modelId Model ID
     * @param updateRequest Update model request
     * @param admin Admin user performing the action
     * @return Updated model response
     */
    public ModelResponse updateModel(Integer modelId, UpdateModelRequest updateRequest, User admin) {
        log.info("Updating model with ID: {} by admin: {}", modelId, admin.getUserId());

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> {
                    log.warn("Model not found with ID: {}", modelId);
                    return new IllegalArgumentException("Model not found");
                });

        ModelStatus oldStatus = model.getStatus();

        if (updateRequest.getFullname() != null && !updateRequest.getFullname().isBlank()) {
            model.setFullname(updateRequest.getFullname());
        }
        if (updateRequest.getUsername() != null && !updateRequest.getUsername().isBlank()) {
            model.setUsername(updateRequest.getUsername());
        }
        if (updateRequest.getProvider() != null && !updateRequest.getProvider().isBlank()) {
            model.setProvider(updateRequest.getProvider());
        }
        if (updateRequest.getParameterCount() != null && !updateRequest.getParameterCount().isBlank()) {
            model.setParameterCount(updateRequest.getParameterCount());
        }
        if (updateRequest.getPricePer1kTokens() != null) {
            model.setPricePer1kTokens(updateRequest.getPricePer1kTokens());
        }
        if (updateRequest.getEndpointUrl() != null && !updateRequest.getEndpointUrl().isBlank()) {
            model.setEndpointUrl(updateRequest.getEndpointUrl());
        }
        if (updateRequest.getStatus() != null) {
            model.setStatus(updateRequest.getStatus());
        }

        model.setUpdatedAt(LocalDateTime.now());
        Model updatedModel = modelRepository.save(model);
        log.info("Model updated successfully with ID: {}", modelId);

        // Audit log
        if (!oldStatus.equals(updateRequest.getStatus())) {
            createAuditLog(updatedModel, oldStatus, updateRequest.getStatus(), admin, "Model status changed");
        } else {
            createAuditLog(updatedModel, oldStatus, updateRequest.getStatus(), admin, "Model updated");
        }

        return mapToResponse(updatedModel);
    }

    /**
     * Change model status
     * 
     * @param modelId Model ID
     * @param newStatus New status
     * @param admin Admin user performing the action
     * @return Updated model response
     */
    public ModelResponse changeModelStatus(Integer modelId, ModelStatus newStatus, User admin) {
        log.info("Changing model status: ID {} to {} by admin: {}", modelId, newStatus, admin.getUserId());

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> {
                    log.warn("Model not found with ID: {}", modelId);
                    return new IllegalArgumentException("Model not found");
                });

        ModelStatus oldStatus = model.getStatus();

        if (oldStatus.equals(newStatus)) {
            log.warn("Model already has status: {}", newStatus);
            throw new IllegalArgumentException("Model already has this status");
        }

        model.setStatus(newStatus);
        model.setUpdatedAt(LocalDateTime.now());
        Model updatedModel = modelRepository.save(model);
        log.info("Model status changed successfully from {} to {} for ID: {}", oldStatus, newStatus, modelId);

        // Audit log
        createAuditLog(updatedModel, oldStatus, newStatus, admin, "Status changed from " + oldStatus + " to " + newStatus);

        return mapToResponse(updatedModel);
    }

    /**
     * Archive model (soft delete - sets status to ARCHIVED)
     * 
     * @param modelId Model ID
     * @param admin Admin user performing the action
     */
    public void archiveModel(Integer modelId, User admin) {
        log.info("Archiving model ID: {} by admin: {}", modelId, admin.getUserId());

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> {
                    log.warn("Model not found with ID: {}", modelId);
                    return new IllegalArgumentException("Model not found");
                });

        ModelStatus oldStatus = model.getStatus();
        model.setStatus(ModelStatus.ARCHIVED);
        model.setUpdatedAt(LocalDateTime.now());
        modelRepository.save(model);
        log.info("Model archived successfully with ID: {}", modelId);

        // Audit log
        createAuditLog(model, oldStatus, ModelStatus.ARCHIVED, admin, "Model archived");
    }

    /**
     * Inactivate model (sets status to INACTIVE)
     * 
     * @param modelId Model ID
     * @param admin Admin user performing the action
     * @return Updated model response
     */
    public ModelResponse inactivateModel(Integer modelId, User admin) {
        log.info("Inactivating model ID: {} by admin: {}", modelId, admin.getUserId());

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> {
                    log.warn("Model not found with ID: {}", modelId);
                    return new IllegalArgumentException("Model not found");
                });

        if (model.getStatus() == ModelStatus.INACTIVE) {
            log.warn("Model already inactive: {}", modelId);
            throw new IllegalArgumentException("Model is already inactive");
        }

        ModelStatus oldStatus = model.getStatus();
        model.setStatus(ModelStatus.INACTIVE);
        model.setUpdatedAt(LocalDateTime.now());
        Model updatedModel = modelRepository.save(model);
        log.info("Model inactivated successfully with ID: {}", modelId);

        // Audit log
        createAuditLog(updatedModel, oldStatus, ModelStatus.INACTIVE, admin, "Model inactivated");

        return mapToResponse(updatedModel);
    }

    /**
     * Hard delete model (permanently removes from database)
     * 
     * @param modelId Model ID
     * @param admin Admin user performing the action
     */
    public void deleteModel(Integer modelId, User admin) {
        log.info("Hard deleting model ID: {} by admin: {}", modelId, admin.getUserId());

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> {
                    log.warn("Model not found with ID: {}", modelId);
                    return new IllegalArgumentException("Model not found");
                });

        // Create audit log before deleting
        createAuditLog(model, model.getStatus(), null, admin, "Model permanently deleted");

        // Hard delete
        modelRepository.deleteById(modelId);
        log.info("Model permanently deleted with ID: {}", modelId);
    }

    /**
     * Get active models for users
     * 
     * @return List of active model responses
     */
    @Transactional(readOnly = true)
    public List<ModelResponse> getActiveModels() {
        log.info("Fetching active models for users");
        return modelRepository.findByStatus(ModelStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create audit log for model status changes
     * 
     * @param model Model entity
     * @param previousStatus Previous status
     * @param newStatus New status
     * @param admin Admin user
     * @param changeReason Reason for change
     */
    private void createAuditLog(Model model, ModelStatus previousStatus, ModelStatus newStatus, User admin, String changeReason) {
        ModelStatusAudit audit = ModelStatusAudit.builder()
                .model(model)
                .oldStatus(previousStatus)
                .newStatus(newStatus)
                .changedBy(admin.getUsername())
                .reason(changeReason)
                .changedAt(LocalDateTime.now())
                .build();

        modelStatusAuditRepository.save(audit);
        log.info("Audit log created for model ID: {} - Status: {} -> {}", model.getModelId(), previousStatus, newStatus);
    }

    /**
     * Get model audit history
     * 
     * @param modelId Model ID
     * @return List of audit records for the model
     */
    @Transactional(readOnly = true)
    public List<ModelStatusAudit> getModelAuditHistory(Integer modelId) {
        log.info("Fetching audit history for model ID: {}", modelId);
        
        // Verify model exists
        if (!modelRepository.existsById(modelId)) {
            log.warn("Model not found with ID: {}", modelId);
            throw new IllegalArgumentException("Model not found");
        }
        
        return modelStatusAuditRepository.findByModelModelIdOrderByChangedAtDesc(modelId);
    }

    /**
     * Map model entity to response DTO
     * 
     * @param model Model entity
     * @return ModelResponse
     */
    private ModelResponse mapToResponse(Model model) {
        return ModelResponse.builder()
                .modelId(model.getModelId())
                .fullname(model.getFullname())
                .username(model.getUsername())
                .provider(model.getProvider())
                .parameterCount(model.getParameterCount())
                .pricePer1kTokens(model.getPricePer1kTokens())
                .status(model.getStatus())
                .endpointUrl(model.getEndpointUrl())
                .type(model.getType())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }
}
