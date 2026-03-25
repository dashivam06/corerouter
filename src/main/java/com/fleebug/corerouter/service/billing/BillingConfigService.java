package com.fleebug.corerouter.service.billing;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.fleebug.corerouter.dto.billing.request.CreateBillingConfigRequest;
import com.fleebug.corerouter.dto.billing.request.UpdateBillingConfigRequest;
import com.fleebug.corerouter.dto.billing.response.BillingConfigResponse;
import com.fleebug.corerouter.entity.billing.BillingConfig;
import com.fleebug.corerouter.entity.model.Model;
import com.fleebug.corerouter.exception.billing.BillingConfigNotFoundException;
import com.fleebug.corerouter.exception.model.ModelNotFoundException;
import com.fleebug.corerouter.repository.billing.BillingConfigRepository;
import com.fleebug.corerouter.repository.model.ModelRepository;
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
public class BillingConfigService {

    private final TelemetryClient telemetryClient;

    private final BillingConfigRepository billingConfigRepository;
    private final ModelRepository modelRepository;
    private final RedisService redisService;

    private static final String BILLING_CACHE_PREFIX = "billing:config:";

    /**
     * Create a new billing configuration for a model.
     *
     * @param request billing config creation request
     * @return created billing config response
     */
    public BillingConfigResponse createBillingConfig(CreateBillingConfigRequest request) {
        telemetryClient.trackTrace("Creating billing config for modelId=" + request.getModelId(), SeverityLevel.Information, Map.of("modelId", String.valueOf(request.getModelId())));

        Model model = modelRepository.findById(request.getModelId())
                .orElseThrow(() -> new ModelNotFoundException(request.getModelId()));

        if (billingConfigRepository.existsByModelModelId(request.getModelId())) {
            throw new IllegalArgumentException("Billing config already exists for model ID " + request.getModelId());
        }

        BillingConfig config = BillingConfig.builder()
                .model(model)
                .pricingType(request.getPricingType())
                .pricingMetadata(request.getPricingMetadata())
                .createdAt(LocalDateTime.now())
                .build();

        BillingConfig saved = billingConfigRepository.save(config);
        telemetryClient.trackTrace("Billing config created with ID=" + saved.getBillingId() + " for modelId=" + request.getModelId(), SeverityLevel.Information, Map.of("billingId", String.valueOf(saved.getBillingId()), "modelId", String.valueOf(request.getModelId())));

        // Invalidate billing config cache
        redisService.deleteFromCache(BILLING_CACHE_PREFIX + request.getModelId());

        return mapToResponse(saved);
    }

    /**
     * Update an existing billing configuration.
     *
     * @param billingId ID of the billing config to update
     * @param request   update request with new values
     * @return updated billing config response
     */
    public BillingConfigResponse updateBillingConfig(Integer billingId, UpdateBillingConfigRequest request) {
        telemetryClient.trackTrace("Updating billing config ID=" + billingId, SeverityLevel.Information, Map.of("billingId", String.valueOf(billingId)));

        BillingConfig config = billingConfigRepository.findById(billingId)
                .orElseThrow(() -> new BillingConfigNotFoundException("Billing config with ID '" + billingId + "' not found"));

        if (request.getPricingType() != null) {
            config.setPricingType(request.getPricingType());
        }
        if (request.getPricingMetadata() != null) {
            config.setPricingMetadata(request.getPricingMetadata());
        }
        config.setUpdatedAt(LocalDateTime.now());

        BillingConfig saved = billingConfigRepository.save(config);
        telemetryClient.trackTrace("Billing config ID=" + saved.getBillingId() + " updated", SeverityLevel.Information, Map.of("billingId", String.valueOf(saved.getBillingId())));

        // Invalidate billing config cache
        redisService.deleteFromCache(BILLING_CACHE_PREFIX + saved.getModel().getModelId());

        return mapToResponse(saved);
    }

    /**
     * Get billing configuration by model ID.
     *
     * @param modelId model ID
     * @return billing config response
     */
    @Transactional(readOnly = true)
    public BillingConfigResponse getBillingConfigByModelId(Integer modelId) {
        telemetryClient.trackTrace("Fetching billing config for modelId=" + modelId, SeverityLevel.Verbose, Map.of("modelId", String.valueOf(modelId)));
        BillingConfig config = billingConfigRepository.findByModelModelId(modelId)
                .orElseThrow(() -> new BillingConfigNotFoundException(modelId));
        return mapToResponse(config);
    }

    /**
     * Get billing configuration by its ID.
     *
     * @param billingId billing config ID
     * @return billing config response
     */
    @Transactional(readOnly = true)
    public BillingConfigResponse getBillingConfigById(Integer billingId) {
        BillingConfig config = billingConfigRepository.findById(billingId)
                .orElseThrow(() -> new BillingConfigNotFoundException("Billing config with ID '" + billingId + "' not found"));
        return mapToResponse(config);
    }

    /**
     * Get all billing configurations.
     *
     * @return list of all billing config responses
     */
    @Transactional(readOnly = true)
    public List<BillingConfigResponse> getAllBillingConfigs() {
        return billingConfigRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Delete a billing configuration.
     *
     * @param billingId ID of the billing config to delete
     */
    public void deleteBillingConfig(Integer billingId) {
        telemetryClient.trackTrace("Deleting billing config ID=" + billingId, SeverityLevel.Information, Map.of("billingId", String.valueOf(billingId)));
        BillingConfig config = billingConfigRepository.findById(billingId)
                .orElseThrow(() -> new BillingConfigNotFoundException("Billing config with ID '" + billingId + "' not found"));
        billingConfigRepository.delete(config);
        telemetryClient.trackTrace("Billing config ID=" + billingId + " deleted", SeverityLevel.Information, Map.of("billingId", String.valueOf(billingId)));

        // Invalidate billing config cache
        redisService.deleteFromCache(BILLING_CACHE_PREFIX + config.getModel().getModelId());
    }

    /**
     * Get billing config entity by model ID (internal use).
     *
     * @param modelId model ID
     * @return BillingConfig entity
     */
    @Transactional(readOnly = true)
    public BillingConfig getBillingConfigEntityByModelId(Integer modelId) {
        return billingConfigRepository.findByModelModelId(modelId)
                .orElseThrow(() -> new BillingConfigNotFoundException(modelId));
    }

    private BillingConfigResponse mapToResponse(BillingConfig config) {
        return BillingConfigResponse.builder()
                .billingId(config.getBillingId())
                .modelId(config.getModel().getModelId())
                .modelName(config.getModel().getFullname())
                .pricingType(config.getPricingType())
                .pricingMetadata(config.getPricingMetadata())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
