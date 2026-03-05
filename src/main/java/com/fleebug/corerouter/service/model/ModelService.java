package com.fleebug.corerouter.service.model;

import com.fleebug.corerouter.dto.documentation.response.ApiDocumentationResponse;
import com.fleebug.corerouter.dto.model.request.CreateModelRequest;
import com.fleebug.corerouter.dto.model.request.UpdateModelRequest;
import com.fleebug.corerouter.dto.model.response.ModelDetailsResponse;
import com.fleebug.corerouter.dto.model.response.ModelResponse;
import com.fleebug.corerouter.entity.documentation.ApiDocumentation;
import com.fleebug.corerouter.entity.model.Model;
import com.fleebug.corerouter.entity.model.ModelStatusAudit;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.model.ModelStatus;
import com.fleebug.corerouter.repository.documentation.ApiDocumentationRepository;
import com.fleebug.corerouter.repository.model.ModelRepository;
import com.fleebug.corerouter.repository.model.ModelStatusAuditRepository;
import com.fleebug.corerouter.service.redis.RedisService;
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
    private final ApiDocumentationRepository documentationRepository;
    private final RedisService redisService;

    private static final String MODEL_CACHE_PREFIX = "model:";

    /**
     * Create a new model.
     *
     * @param createRequest model creation request
     * @param admin         admin user performing the action
     * @return created model response
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
                .status(ModelStatus.ACTIVE)
                .endpointUrl(createRequest.getEndpointUrl())
                .description(createRequest.getDescription())
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
     * Get all models.
     *
     * @return list of all model responses
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
     * Get models filtered by status.
     *
     * @param status model status to filter by
     * @return list of matching model responses
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
     * Get a model by its ID.
     *
     * @param modelId model ID
     * @return model response
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
     * Get model details including associated API documentation.
     *
     * @param modelId model ID
     * @return model details response with documentation
     */
    @Transactional(readOnly = true)
    public ModelDetailsResponse getModelDetailsWithDocumentation(Integer modelId) {
        log.info("Fetching model details with documentation for ID: {}", modelId);
        
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> {
                    log.warn("Model not found with ID: {}", modelId);
                    return new IllegalArgumentException("Model not found");
                });

        // Fetch documentation for this model
        List<ApiDocumentation> docs = documentationRepository.findByModel_ModelId(modelId);
        List<ApiDocumentationResponse> docResponses = docs.stream()
                .map(this::mapDocToResponse)
                .collect(Collectors.toList());

        return mapToDetailsResponse(model, docResponses);
    }

    /**
     * Update model fields.
     *
     * @param modelId       model ID
     * @param updateRequest update request with new values
     * @param admin         admin user performing the action
     * @return updated model response
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
        if (updateRequest.getEndpointUrl() != null && !updateRequest.getEndpointUrl().isBlank()) {
            model.setEndpointUrl(updateRequest.getEndpointUrl());
        }
        if (updateRequest.getDescription() != null && !updateRequest.getDescription().isBlank()) {
            model.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getStatus() != null) {
            model.setStatus(updateRequest.getStatus());
        }

        model.setUpdatedAt(LocalDateTime.now());
        Model updatedModel = modelRepository.save(model);
        log.info("Model updated successfully with ID: {}", modelId);

        // Invalidate model cache
        redisService.deleteFromCache(MODEL_CACHE_PREFIX + modelId);

        // Audit log
        if (!oldStatus.equals(updateRequest.getStatus())) {
            createAuditLog(updatedModel, oldStatus, updateRequest.getStatus(), admin, "Model status changed");
        } else {
            createAuditLog(updatedModel, oldStatus, updateRequest.getStatus(), admin, "Model updated");
        }

        return mapToResponse(updatedModel);
    }

    /**
     * Change model status.
     *
     * @param modelId   model ID
     * @param newStatus new status to set
     * @param admin     admin user performing the action
     * @return updated model response
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

        // Invalidate model cache
        redisService.deleteFromCache(MODEL_CACHE_PREFIX + modelId);

        // Audit log
        createAuditLog(updatedModel, oldStatus, newStatus, admin, "Status changed from " + oldStatus + " to " + newStatus);

        return mapToResponse(updatedModel);
    }

    /**
     * Archive a model.
     *
     * @param modelId model ID
     * @param admin   admin user performing the action
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

        // Invalidate model cache
        redisService.deleteFromCache(MODEL_CACHE_PREFIX + modelId);

        // Audit log
        createAuditLog(model, oldStatus, ModelStatus.ARCHIVED, admin, "Model archived");
    }

    /**
     * Inactivate a model.
     *
     * @param modelId model ID
     * @param admin   admin user performing the action
     * @return updated model response
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

        // Invalidate model cache
        redisService.deleteFromCache(MODEL_CACHE_PREFIX + modelId);

        // Audit log
        createAuditLog(updatedModel, oldStatus, ModelStatus.INACTIVE, admin, "Model inactivated");

        return mapToResponse(updatedModel);
    }

    /**
     * Permanently delete a model.
     *
     * @param modelId model ID
     * @param admin   admin user performing the action
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

        // Invalidate model cache
        redisService.deleteFromCache(MODEL_CACHE_PREFIX + modelId);
    }

    /**
     * Get all active models.
     *
     * @return list of active model responses
     */
    @Transactional(readOnly = true)
    public List<ModelResponse> getActiveModels() {
        log.info("Fetching active models for users");
        return modelRepository.findByStatus(ModelStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void createAuditLog(Model model, ModelStatus previousStatus, ModelStatus newStatus, User admin, String changeReason) {
        ModelStatusAudit audit = ModelStatusAudit.builder()
                .model(model)
                .oldStatus(previousStatus)
                .newStatus(newStatus)
                .changedBy(admin.getFullName())
                .reason(changeReason)
                .changedAt(LocalDateTime.now())
                .build();

        modelStatusAuditRepository.save(audit);
        log.info("Audit log created for model ID: {} - Status: {} -> {}", model.getModelId(), previousStatus, newStatus);
    }

    /**
     * Get status audit history for a model.
     *
     * @param modelId model ID
     * @return list of audit entries ordered by most recent first
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

    private ModelResponse mapToResponse(Model model) {
        return ModelResponse.builder()
                .modelId(model.getModelId())
                .fullname(model.getFullname())
                .username(model.getUsername())
                .provider(model.getProvider())
                .status(model.getStatus())
                .endpointUrl(model.getEndpointUrl())
                .type(model.getType())
                .description(model.getDescription())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }

    private ModelDetailsResponse mapToDetailsResponse(Model model, List<ApiDocumentationResponse> documentation) {
        return ModelDetailsResponse.builder()
                .modelId(model.getModelId())
                .fullname(model.getFullname())
                .username(model.getUsername())
                .provider(model.getProvider())
                .status(model.getStatus())
                .endpointUrl(model.getEndpointUrl())
                .type(model.getType())
                .description(model.getDescription())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .documentation(documentation)
                .build();
    }

    private ApiDocumentationResponse mapDocToResponse(ApiDocumentation doc) {
        return ApiDocumentationResponse.builder()
                .docId(doc.getDocId())
                .title(doc.getTitle())
                .content(doc.getContent())
                .modelId(doc.getModel().getModelId())
                .modelName(doc.getModel().getFullname())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
