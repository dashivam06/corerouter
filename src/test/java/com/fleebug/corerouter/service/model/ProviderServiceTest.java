package com.fleebug.corerouter.service.model;

import com.fleebug.corerouter.dto.model.request.CreateProviderRequest;
import com.fleebug.corerouter.dto.model.request.UpdateProviderRequest;
import com.fleebug.corerouter.dto.model.response.ProviderResponse;
import com.fleebug.corerouter.entity.model.Model;
import com.fleebug.corerouter.entity.model.Provider;
import com.fleebug.corerouter.enums.model.ModelStatus;
import com.fleebug.corerouter.enums.model.ProviderStatus;
import com.fleebug.corerouter.exception.model.ProviderNotFoundException;
import com.fleebug.corerouter.repository.model.ModelRepository;
import com.fleebug.corerouter.repository.model.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private ModelRepository modelRepository;

    @InjectMocks
    private ProviderService providerService;

    private Provider testProvider;

    @BeforeEach
    void setUp() {
        testProvider = Provider.builder()
                .providerId(1)
                .providerName("OpenAI")
                .companyName("OpenAI Inc.")
                .providerCountry("USA")
                .status(ProviderStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    // Tests that a new provider is created successfully when the name is unique
    void createProvider_ReturnsCreatedProvider() {
        CreateProviderRequest request = new CreateProviderRequest();
        request.setProviderName("Anthropic");
        request.setCompanyName("Anthropic PBC");
        request.setProviderCountry("USA");

        when(providerRepository.existsByProviderName("Anthropic")).thenReturn(false);
        when(providerRepository.save(any(Provider.class))).thenAnswer(inv -> inv.getArgument(0));

        ProviderResponse response = providerService.createProvider(request);

        assertNotNull(response);
        assertEquals("Anthropic", response.getProviderName());
        assertEquals("Anthropic PBC", response.getCompanyName());
        assertEquals("USA", response.getProviderCountry());
        assertEquals(ProviderStatus.ACTIVE, response.getStatus()); // Assuming default is ACTIVE
    }

    @Test
    // Tests that an exception is thrown when creating a provider with a duplicate name
    void createProvider_DuplicateName_ThrowsException() {
        CreateProviderRequest request = new CreateProviderRequest();
        request.setProviderName("OpenAI");

        when(providerRepository.existsByProviderName("OpenAI")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> providerService.createProvider(request));
        verify(providerRepository, never()).save(any());
    }

    @Test
    // Tests that updating an existing provider succeeds and updates correct fields
    void updateProvider_ReturnsUpdatedProvider() {
        UpdateProviderRequest request = new UpdateProviderRequest();
        request.setProviderName("OpenAI Updated");
        request.setCompanyName("OpenAI LLC");

        when(providerRepository.findById(1)).thenReturn(Optional.of(testProvider));
        when(providerRepository.existsByProviderName("OpenAI Updated")).thenReturn(false);
        when(providerRepository.save(any(Provider.class))).thenAnswer(inv -> inv.getArgument(0));

        ProviderResponse response = providerService.updateProvider(1, request);

        assertNotNull(response);
        assertEquals("OpenAI Updated", response.getProviderName());
        assertEquals("OpenAI LLC", response.getCompanyName());
        assertEquals("USA", response.getProviderCountry()); // Unchanged
    }

    @Test
    // Tests that updating a provider to an existing name throws an exception
    void updateProvider_DuplicateName_ThrowsException() {
        UpdateProviderRequest request = new UpdateProviderRequest();
        request.setProviderName("Existing Provider");

        when(providerRepository.findById(1)).thenReturn(Optional.of(testProvider));
        when(providerRepository.existsByProviderName("Existing Provider")).
            thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> providerService.
            updateProvider(1, request));
        verify(providerRepository, never()).save(any());
    }

    @Test
    // Tests that changing a provider status to DISABLED also disables its associated active models
    void changeProviderStatus_ToDisabled_AlsoDisablesModels() {
        Model activeModel = Model.builder()
                .modelId(10)
                .status(ModelStatus.ACTIVE)
                .build();
        Model inactiveModel = Model.builder()
                .modelId(11)
                .status(ModelStatus.INACTIVE)
                .build();

        when(providerRepository.findById(1)).thenReturn(Optional.of(testProvider));
        when(modelRepository.findByProvider_ProviderId(1)).thenReturn(List.of(activeModel, inactiveModel));
        when(providerRepository.save(any(Provider.class))).thenAnswer(inv -> inv.getArgument(0));
        
        ProviderResponse response = providerService.changeProviderStatus(1, ProviderStatus.DISABLED);

        assertNotNull(response);
        assertEquals(ProviderStatus.DISABLED, response.getStatus());

        // Verifies models are updated to INACTIVE
        assertEquals(ModelStatus.INACTIVE, activeModel.getStatus());
        verify(modelRepository).saveAll(anyList());
    }

    @Test
    // Tests that changing provider status on a non-existent provider throws ProviderNotFoundException
    void changeProviderStatus_NotFound_ThrowsException() {
        when(providerRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ProviderNotFoundException.class, () -> providerService
            .changeProviderStatus(99, ProviderStatus.ACTIVE));
        verify(providerRepository, never()).save(any());
    }
}
