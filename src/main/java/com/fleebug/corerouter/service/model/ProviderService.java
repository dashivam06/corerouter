package com.fleebug.corerouter.service.model;

import com.fleebug.corerouter.dto.model.request.CreateProviderRequest;
import com.fleebug.corerouter.dto.model.request.UpdateProviderRequest;
import com.fleebug.corerouter.dto.model.response.ProviderResponse;
import com.fleebug.corerouter.entity.model.Provider;
import com.fleebug.corerouter.enums.model.ProviderStatus;
import com.fleebug.corerouter.exception.model.ProviderNotFoundException;
import com.fleebug.corerouter.repository.model.ProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProviderService {

    private final ProviderRepository providerRepository;

    /**
     * Create a new provider
     */
    @Transactional
    public ProviderResponse createProvider(CreateProviderRequest request) {
        if (providerRepository.existsByProviderName(request.getProviderName())) {
            throw new IllegalArgumentException("Provider with name '" + request.getProviderName() + "' already exists");
        }

        byte[] logoBytes = null;
        if (request.getLogo() != null && !request.getLogo().isBlank()) {
            logoBytes = request.getLogo().getBytes(StandardCharsets.UTF_8);
        }

        Provider provider = Provider.builder()
                .providerName(request.getProviderName())
                .providerCountry(request.getProviderCountry())
                .companyName(request.getCompanyName())
                .logo(logoBytes)
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
            provider.setLogo(request.getLogo().getBytes(StandardCharsets.UTF_8));
        }

        Provider updatedProvider = providerRepository.save(provider);
        return convertToResponse(updatedProvider);
    }

    /**
     * Change provider status (activate, disable, suspend, etc.)
     */
    @Transactional
    public ProviderResponse changeProviderStatus(Integer providerId, ProviderStatus newStatus) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ProviderNotFoundException(providerId));

        provider.setStatus(newStatus);

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
        String logoUrl = null;
        if (provider.getLogo() != null && provider.getLogo().length > 0) {
            logoUrl = new String(provider.getLogo(), StandardCharsets.UTF_8);
        }

        return ProviderResponse.builder()
                .providerId(provider.getProviderId())
                .providerName(provider.getProviderName())
                .providerCountry(provider.getProviderCountry())
                .companyName(provider.getCompanyName())
                .logo(logoUrl)
                .status(provider.getStatus())
                .createdAt(provider.getCreatedAt())
                .updatedAt(provider.getUpdatedAt())
                .build();
    }
}
