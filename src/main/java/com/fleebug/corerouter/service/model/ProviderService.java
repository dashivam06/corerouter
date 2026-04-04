package com.fleebug.corerouter.service.model;

import com.fleebug.corerouter.dto.model.request.CreateProviderRequest;
import com.fleebug.corerouter.dto.model.request.UpdateProviderRequest;
import com.fleebug.corerouter.dto.model.response.ProviderResponse;
import com.fleebug.corerouter.entity.model.Provider;
import com.fleebug.corerouter.entity.model.Model;
import com.fleebug.corerouter.enums.model.ProviderStatus;
import com.fleebug.corerouter.enums.model.ModelStatus;
import com.fleebug.corerouter.exception.model.ProviderNotFoundException;
import com.fleebug.corerouter.repository.model.ProviderRepository;
import com.fleebug.corerouter.repository.model.ModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProviderService {

    private final ProviderRepository providerRepository;
    private final ModelRepository modelRepository;

    /**
     * Create a new provider
     */
    @Transactional
    public ProviderResponse createProvider(CreateProviderRequest request) {
        if (providerRepository.existsByProviderName(request.getProviderName())) {
            throw new IllegalArgumentException("Provider with name '" + request.getProviderName() + "' already exists");
        }

        Provider provider = Provider.builder()
                .providerName(request.getProviderName())
                .providerCountry(request.getProviderCountry())
                .companyName(request.getCompanyName())
                .logo(request.getLogo())
                .status(ProviderStatus.ACTIVE)
                .build();

        Provider savedProvider = providerRepository.save(provider);
        return convertToResponse(savedProvider);
    }

    /**
     * Get provider by ID
     */
    public ProviderResponse getProviderById(Integer providerId) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ProviderNotFoundException(providerId));
        return convertToResponse(provider);
    }

    /**
     * Get provider by name
     */
    public ProviderResponse getProviderByName(String providerName) {
        Provider provider = providerRepository.findByProviderName(providerName)
                .orElseThrow(() -> new ProviderNotFoundException("name", providerName));
        return convertToResponse(provider);
    }

    /**
     * Get all providers
     */
    public List<ProviderResponse> getAllProviders() {
        return providerRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all active providers
     */
    public List<ProviderResponse> getActiveProviders() {
        return providerRepository.findByStatus(ProviderStatus.ACTIVE)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update provider
     */
    @Transactional
    public ProviderResponse updateProvider(Integer providerId, UpdateProviderRequest request) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ProviderNotFoundException(providerId));

        if (request.getProviderName() != null && !request.getProviderName().isEmpty()) {
            // Check if new name is already taken by another provider
            if (!request.getProviderName().equals(provider.getProviderName())) {
                if (providerRepository.existsByProviderName(request.getProviderName())) {
                    throw new IllegalArgumentException("Provider with name '" + request.getProviderName() + "' already exists");
                }
            }
            provider.setProviderName(request.getProviderName());
        }

        if (request.getProviderCountry() != null && !request.getProviderCountry().isEmpty()) {
            provider.setProviderCountry(request.getProviderCountry());
        }

        if (request.getCompanyName() != null && !request.getCompanyName().isEmpty()) {
            provider.setCompanyName(request.getCompanyName());
        }

        if (request.getLogo() != null && !request.getLogo().isBlank()) {
            provider.setLogo(request.getLogo());
        }

        Provider updatedProvider = providerRepository.save(provider);
        return convertToResponse(updatedProvider);
    }

    /**
     * Change provider status (activate, disable, suspend, etc.)
     * When disabling, suspending, or deleting a provider, all associated models are also disabled.
     */
    @Transactional
    public ProviderResponse changeProviderStatus(Integer providerId, ProviderStatus newStatus) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ProviderNotFoundException(providerId));

        provider.setStatus(newStatus);
        provider.setUpdatedAt(LocalDateTime.now());

        // When provider is disabled, suspended, or deleted, disable all its models
        if (newStatus == ProviderStatus.DISABLED || newStatus == ProviderStatus.SUSPENDED || newStatus == ProviderStatus.DELETED) {
            List<Model> modelsToDisable = modelRepository.findByProviderId(providerId);
            for (Model model : modelsToDisable) {
                if (!model.getStatus().equals(ModelStatus.INACTIVE)) {
                    model.setStatus(ModelStatus.INACTIVE);
                    model.setUpdatedAt(LocalDateTime.now());
                }
            }
            if (!modelsToDisable.isEmpty()) {
                modelRepository.saveAll(modelsToDisable);
            }
        }

        Provider updatedProvider = providerRepository.save(provider);
        return convertToResponse(updatedProvider);
    }

    /**
     * Delete provider (soft delete by setting status to DELETED)
     */
    @Transactional
    public void deleteProvider(Integer providerId) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ProviderNotFoundException(providerId));

        provider.setStatus(ProviderStatus.DELETED);
        providerRepository.save(provider);
    }

    /**
     * Get provider entity (internal use)
     */
    public Provider getProviderEntity(Integer providerId) {
        return providerRepository.findById(providerId)
                .orElseThrow(() -> new ProviderNotFoundException(providerId));
    }

    /**
     * Convert Provider entity to response DTO
     */
    private ProviderResponse convertToResponse(Provider provider) {
        return ProviderResponse.builder()
                .providerId(provider.getProviderId())
                .providerName(provider.getProviderName())
                .providerCountry(provider.getProviderCountry())
                .companyName(provider.getCompanyName())
                .logo(provider.getLogo())
                .status(provider.getStatus())
                .createdAt(provider.getCreatedAt())
                .updatedAt(provider.getUpdatedAt())
                .build();
    }
}
