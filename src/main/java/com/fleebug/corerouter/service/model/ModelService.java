package com.fleebug.corerouter.service.model;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.fleebug.corerouter.dto.documentation.response.ApiDocumentationResponse;
import com.fleebug.corerouter.dto.model.response.AdminModelInsightsResponse;
import com.fleebug.corerouter.dto.model.request.CreateModelRequest;
import com.fleebug.corerouter.dto.model.request.UpdateModelRequest;
import com.fleebug.corerouter.dto.model.response.ModelDetailsResponse;
import com.fleebug.corerouter.dto.model.response.ModelResponse;
import com.fleebug.corerouter.entity.documentation.ApiDocumentation;
import com.fleebug.corerouter.entity.model.Model;
import com.fleebug.corerouter.entity.model.ModelStatusAudit;
import com.fleebug.corerouter.entity.model.Provider;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.model.ModelStatus;
import com.fleebug.corerouter.repository.documentation.ApiDocumentationRepository;
import com.fleebug.corerouter.repository.model.ModelRepository;
import com.fleebug.corerouter.repository.model.ProviderRepository;
import com.fleebug.corerouter.repository.model.ModelStatusAuditRepository;
import com.fleebug.corerouter.service.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ModelService {

    private final TelemetryClient telemetryClient;
    private final ModelRepository modelRepository;
    private final ModelStatusAuditRepository modelStatusAuditRepository;
    private final ProviderRepository providerRepository;
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
        telemetryClient.trackTrace("Creating new model: " + createRequest.getFullname() + " by admin: " + admin.getUserId(), SeverityLevel.Information, Map.of("modelName", createRequest.getFullname(), "adminId", String.valueOf(admin.getUserId())));

        if (modelRepository.existsByFullname(createRequest.getFullname())) {
            telemetryClient.trackTrace("Model with name already exists: " + createRequest.getFullname(), SeverityLevel.Information, Map.of("modelName", createRequest.getFullname()));
            throw new IllegalArgumentException("Model with this name already exists");
        }

        Provider provider = providerRepository.findByProviderName(createRequest.getProvider())
            .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + createRequest.getProvider()));

        Model model = Model.builder()
                .fullname(createRequest.getFullname())
                .username(createRequest.getUsername())
            .provider(provider)
                .status(ModelStatus.ACTIVE)
                .endpointUrl(createRequest.getEndpointUrl())
                .description(createRequest.getDescription())
                .type(createRequest.getType())
                .createdAt(LocalDateTime.now())
                .build();

        Model savedModel = modelRepository.save(model);
        telemetryClient.trackTrace("Model created successfully with ID: " + savedModel.getModelId(), SeverityLevel.Information, Map.of("modelId", String.valueOf(savedModel.getModelId())));

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
        // telemetryClient.trackTrace("Fetching all models", SeverityLevel.Verbose, null);
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
        // telemetryClient.trackTrace("Fetching models with status: " + status, SeverityLevel.Verbose, Map.of("status", status.toString()));
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
        // telemetryClient.trackTrace("Fetching model with ID: " + modelId, SeverityLevel.Verbose, Map.of("modelId", String.valueOf(modelId)));
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> {
                    telemetryClient.trackTrace("Model not found with ID: " + modelId, SeverityLevel.Information, Map.of("modelId", String.valueOf(modelId)));
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
        // telemetryClient.trackTrace("Fetching model details with documentation for ID: " + modelId, SeverityLevel.Verbose, Map.of("modelId", String.valueOf(modelId)));
        
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> {
                    telemetryClient.trackTrace("Model not found with ID: " + modelId, SeverityLevel.Information, Map.of("modelId", String.valueOf(modelId)));
                    return new IllegalArgumentException("Model not found");
                });

        // Fetch documentation for this model
        List<ApiDocumentation> docs = documentationRepository.findByModel_ModelIdAndActiveTrue(modelId);
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
        telemetryClient.trackTrace("Updating model with ID: " + modelId + " by admin: " + admin.getUserId(), SeverityLevel.Information, Map.of("modelId", String.valueOf(modelId), "adminId", String.valueOf(admin.getUserId())));

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> {
                    telemetryClient.trackTrace("Model not found with ID: " + modelId, SeverityLevel.Information, Map.of("modelId", String.valueOf(modelId)));
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
            Provider provider = providerRepository.findByProviderName(updateRequest.getProvider())
                    .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + updateRequest.getProvider()));
            model.setProvider(provider);
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
        telemetryClient.trackTrace("Model updated successfully with ID: " + modelId, SeverityLevel.Information, Map.of("modelId", String.valueOf(modelId)));

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
        telemetryClient.trackTrace("Changing model status: ID " + modelId + " to " + newStatus + " by admin: " + admin.getUserId(), SeverityLevel.Information, Map.of("modelId", String.valueOf(modelId), "newStatus", newStatus.toString(), "adminId", String.valueOf(admin.getUserId())));

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> {
                    telemetryClient.trackTrace("Model not found with ID: " + modelId, SeverityLevel.Information, Map.of("modelId", String.valueOf(modelId)));
                    return new IllegalArgumentException("Model not found");
                });

        ModelStatus oldStatus = model.getStatus();

        if (oldStatus.equals(newStatus)) {
            telemetryClient.trackTrace("Model already has status: " + newStatus, SeverityLevel.Information, Map.of("status", newStatus.toString()));
            throw new IllegalArgumentException("Model already has this status");
        }

        model.setStatus(newStatus);
        model.setUpdatedAt(LocalDateTime.now());
        Model updatedModel = modelRepository.save(model);
        telemetryClient.trackTrace("Model status changed successfully from " + oldStatus + " to " + newStatus + " for ID: " + modelId, SeverityLevel.Information, Map.of("modelId", String.valueOf(modelId), "oldStatus", oldStatus.toString(), "newStatus", newStatus.toString()));

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
        telemetryClient.trackTrace("Archiving model ID: " + modelId + " by admin: " + admin.getUserId(), SeverityLevel.Information, Map.of("modelId", String.valueOf(modelId), "adminId", String.valueOf(admin.getUserId())));

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> {
                    telemetryClient.trackTrace("Model not found", SeverityLevel.Information, Map.of("modelId", String.valueOf(modelId)));
                    return new IllegalArgumentException("Model not found");
                });

        ModelStatus oldStatus = model.getStatus();
        model.setStatus(ModelStatus.ARCHIVED);
        model.setUpdatedAt(LocalDateTime.now());
        modelRepository.save(model);
        telemetryClient.trackTrace("Model archived successfully", SeverityLevel.Information, Map.of("modelId", String.valueOf(modelId)));

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
        telemetryClient.trackTrace("Inactivating model ID: " + modelId + " by admin: " + admin.getUserId(), SeverityLevel.Information, Map.of("modelId", String.valueOf(modelId), "adminId", String.valueOf(admin.getUserId())));

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> {
                    telemetryClient.trackTrace("Model not found with ID: " + modelId, SeverityLevel.Information, Map.of("modelId", String.valueOf(modelId)));
                    return new IllegalArgumentException("Model not found");
                });

        if (model.getStatus() == ModelStatus.INACTIVE) {
            telemetryClient.trackTrace("Model already inactive: " + modelId, SeverityLevel.Information, Map.of("modelId", String.valueOf(modelId)));
            throw new IllegalArgumentException("Model is already inactive");
        }

        ModelStatus oldStatus = model.getStatus();
        model.setStatus(ModelStatus.INACTIVE);
        model.setUpdatedAt(LocalDateTime.now());
        Model updatedModel = modelRepository.save(model);
        telemetryClient.trackTrace("Model inactivated successfully with ID: " + modelId, SeverityLevel.Information, Map.of("modelId", String.valueOf(modelId)));

        // Invalidate model cache
        redisService.deleteFromCache(MODEL_CACHE_PREFIX + modelId);

        // Audit log
        createAuditLog(updatedModel, oldStatus, ModelStatus.INACTIVE, admin, "Model inactivated");

        return mapToResponse(updatedModel);
    }

    /**
     * Soft delete a model by archiving it.
     *
     * @param modelId model ID
     * @param admin   admin user performing the action
     */
    public void deleteModel(Integer modelId, User admin) {
        telemetryClient.trackTrace("Soft deleting model ID: " + modelId + " by admin: " + admin.getUserId(), SeverityLevel.Information, Map.of("modelId", String.valueOf(modelId), "adminId", String.valueOf(admin.getUserId())));

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> {
                    telemetryClient.trackTrace("Model not found with ID: " + modelId, SeverityLevel.Information, Map.of("modelId", String.valueOf(modelId)));
                    return new IllegalArgumentException("Model not found");
                });

        ModelStatus oldStatus = model.getStatus();
        if (oldStatus != ModelStatus.ARCHIVED) {
            model.setStatus(ModelStatus.ARCHIVED);
            model.setUpdatedAt(LocalDateTime.now());
            modelRepository.save(model);
            createAuditLog(model, oldStatus, ModelStatus.ARCHIVED, admin, "Model soft deleted");
        }

        telemetryClient.trackTrace("Model soft deleted with ID: " + modelId, SeverityLevel.Information, Map.of("modelId", String.valueOf(modelId)));

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
        // telemetryClient.trackTrace("Fetching active models for users", SeverityLevel.Verbose, null);
        return modelRepository.findByStatus(ModelStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminModelInsightsResponse getAdminInsights() {
        long totalModels = modelRepository.count();
        long activeModels = modelRepository.countByStatus(ModelStatus.ACTIVE);
        long providers = providerRepository.count();

        return AdminModelInsightsResponse.builder()
                .totalModels(totalModels)
                .activeModels(activeModels)
                .providers(providers)
                .build();
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
        telemetryClient.trackTrace("Audit log created for model ID: " + model.getModelId() + " - Status: " + previousStatus + " -> " + newStatus, SeverityLevel.Information, Map.of("modelId", String.valueOf(model.getModelId()), "previousStatus", String.valueOf(previousStatus), "newStatus", String.valueOf(newStatus)));
    }

    /**
     * Get status audit history for a model.
     *
     * @param modelId model ID
     * @return list of audit entries ordered by most recent first
     */
    @Transactional(readOnly = true)
    public List<ModelStatusAudit> getModelAuditHistory(Integer modelId) {
        // telemetryClient.trackTrace("Fetching audit history for model ID: " + modelId, SeverityLevel.Verbose, Map.of("modelId", String.valueOf(modelId)));
        
        // Verify model exists
        if (!modelRepository.existsById(modelId)) {
            telemetryClient.trackTrace("Model not found with ID: " + modelId, SeverityLevel.Information, Map.of("modelId", String.valueOf(modelId)));
            throw new IllegalArgumentException("Model not found");
        }
        
        return modelStatusAuditRepository.findByModelModelIdOrderByChangedAtDesc(modelId);
    }

    private ModelResponse mapToResponse(Model model) {
        return ModelResponse.builder()
                .modelId(model.getModelId())
                .fullname(model.getFullname())
                .username(model.getUsername())
            .provider(model.getProvider() != null ? model.getProvider().getProviderName() : null)
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
            .provider(model.getProvider() != null ? model.getProvider().getProviderName() : null)
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
