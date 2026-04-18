package com.fleebug.corerouter.service.model;

import com.microsoft.applicationinsights.TelemetryClient;
import com.fleebug.corerouter.dto.model.request.CreateModelRequest;
import com.fleebug.corerouter.dto.model.request.UpdateModelRequest;
import com.fleebug.corerouter.dto.model.response.ModelResponse;
import com.fleebug.corerouter.entity.model.Model;
import com.fleebug.corerouter.entity.model.ModelStatusAudit;
import com.fleebug.corerouter.entity.model.Provider;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.model.ModelStatus;
import com.fleebug.corerouter.enums.model.ModelType;
import com.fleebug.corerouter.repository.model.ModelRepository;
import com.fleebug.corerouter.repository.model.ModelStatusAuditRepository;
import com.fleebug.corerouter.repository.model.ProviderRepository;
import com.fleebug.corerouter.repository.documentation.ApiDocumentationRepository;
import com.fleebug.corerouter.service.redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelServiceTest {

    @Mock
    private TelemetryClient telemetryClient;

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private ModelStatusAuditRepository modelStatusAuditRepository;

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private ApiDocumentationRepository documentationRepository;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private ModelService modelService;

    private User testAdmin;
    private Provider testProvider;
    private Model testModel;

    @BeforeEach
    void setUp() {
        testAdmin = User.builder().userId(1).fullName("Admin User").build();

        testProvider = Provider.builder()
                .providerId(1)
                .providerName("OpenAI")
                .build();

        testModel = Model.builder()
                .modelId(10)
                .fullname("GPT-4o")
                .username("gpt-4o")
                .provider(testProvider)
                .status(ModelStatus.ACTIVE)
                .type(ModelType.LLM)
                .createdAt(LocalDateTime.now())
                .endpointUrl("https://api.openai.com/v1/chat/completions")
                .build();
    }

    @Test
    // Tests that an entirely new model configuration is securely created
    void createModel_ReturnsCreatedModelSuccessfully() {
        CreateModelRequest request = new CreateModelRequest();
        request.setFullname("GPT-4o");
        request.setUsername("gpt-4o");
        request.setProvider("OpenAI");
        request.setEndpointUrl("https://api.openai.com");
        request.setType(ModelType.LLM);

        when(modelRepository.existsByFullname("GPT-4o")).thenReturn(false);
        when(providerRepository.findByProviderName("OpenAI")).thenReturn(Optional.of(testProvider));
        
        when(modelRepository.save(any(Model.class))).thenAnswer(inv -> {
            Model saved = inv.getArgument(0);
            saved.setModelId(10);
            return saved;
        });

        ModelResponse response = modelService.createModel(request, testAdmin);

        assertNotNull(response);
        assertEquals("GPT-4o", response.getFullname());
        assertEquals("OpenAI", response.getProvider());
        assertEquals(ModelStatus.ACTIVE, response.getStatus());

        verify(modelStatusAuditRepository, times(1)).save(any(ModelStatusAudit.class));
    }

    @Test
    // Tests that creating a model correctly rejects duplicated names
    void createModel_DuplicateName_ThrowsException() {
        CreateModelRequest request = new CreateModelRequest();
        request.setFullname("GPT-4o");

        when(modelRepository.existsByFullname("GPT-4o")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
                () -> modelService.createModel(request, testAdmin));
        
        assertEquals("Model with this name already exists", ex.getMessage());
        verify(modelRepository, never()).save(any());
    }

    @Test
    // Tests that an existing model correctly patches new updating information
    void updateModel_UpdatesFieldsSuccessfully() {
        UpdateModelRequest request = new UpdateModelRequest();
        request.setFullname("GPT-4o Turbo");

        when(modelRepository.findById(10)).thenReturn(Optional.of(testModel));
        when(modelRepository.save(any(Model.class))).thenAnswer(inv -> inv.getArgument(0));

        ModelResponse response = modelService.updateModel(10, request, testAdmin);

        assertNotNull(response);
        assertEquals("GPT-4o Turbo", response.getFullname());
        assertEquals("gpt-4o", response.getUsername()); // remains unchanged
    }

    @Test
    // Tests that modifying a model status saves changes and wipes cache records
    void changeModelStatus_UpdatesStatusAndClearsCache() {
        when(modelRepository.findById(10)).thenReturn(Optional.of(testModel));
        when(modelRepository.save(any(Model.class))).thenAnswer(inv -> inv.getArgument(0));

        ModelResponse response = modelService.changeModelStatus(10, ModelStatus.INACTIVE, testAdmin);

        assertNotNull(response);
        assertEquals(ModelStatus.INACTIVE, response.getStatus());
        
        verify(modelStatusAuditRepository).save(any(ModelStatusAudit.class));
        verify(redisService).deleteFromCache(contains("10")); // verifies cache purging
    }

    @Test
    // Tests that archiving a model triggers safe soft logical archiving actions
    void archiveModel_SoftArchivesModelSuccessfully() {
        when(modelRepository.findById(10)).thenReturn(Optional.of(testModel));
        when(modelRepository.save(any(Model.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> modelService.archiveModel(10, testAdmin));

        assertEquals(ModelStatus.ARCHIVED, testModel.getStatus());
        verify(modelStatusAuditRepository).save(any(ModelStatusAudit.class));
    }
}
