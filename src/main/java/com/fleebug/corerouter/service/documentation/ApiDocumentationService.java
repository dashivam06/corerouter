package com.fleebug.corerouter.service.documentation;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.fleebug.corerouter.dto.documentation.request.CreateApiDocumentationRequest;
import com.fleebug.corerouter.dto.documentation.request.UpdateApiDocumentationRequest;
import com.fleebug.corerouter.dto.documentation.response.ApiDocumentationResponse;
import com.fleebug.corerouter.entity.documentation.ApiDocumentation;
import com.fleebug.corerouter.entity.model.Model;
import com.fleebug.corerouter.repository.documentation.ApiDocumentationRepository;
import com.fleebug.corerouter.repository.model.ModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ApiDocumentationService {

    private final TelemetryClient telemetryClient;
    private final ApiDocumentationRepository documentationRepository;
    private final ModelRepository modelRepository;

    /**
     * Create documentation for a model
     * 
     * @param modelId Model ID
     * @param request Create documentation request
     * @return Created documentation response
     */
    public ApiDocumentationResponse createDocumentation(Integer modelId, CreateApiDocumentationRequest request) {
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> {
                    Map<String, String> properties = new HashMap<>();
                    properties.put("modelId", String.valueOf(modelId));
                    telemetryClient.trackTrace("Model not found for documentation creation", SeverityLevel.Information, properties);
                    return new IllegalArgumentException("Model not found");
                });

        ApiDocumentation documentation = ApiDocumentation.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .model(model)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        ApiDocumentation savedDoc = documentationRepository.save(documentation);
        
        Map<String, String> properties = new HashMap<>();
        properties.put("docId", String.valueOf(savedDoc.getDocId()));
        properties.put("modelId", String.valueOf(modelId));
        telemetryClient.trackEvent("DocumentationCreated", properties, null);

        return mapToResponse(savedDoc);
    }

    /**
     * Get all documentation for a model
     * 
     * @param modelId Model ID
     * @return List of documentation responses
     */
    @Transactional(readOnly = true)
    public List<ApiDocumentationResponse> getDocumentationByModelId(Integer modelId) {
        // Verify model exists
        if (!modelRepository.existsById(modelId)) {
            Map<String, String> properties = new HashMap<>();
            properties.put("modelId", String.valueOf(modelId));
            telemetryClient.trackTrace("Model not found for getting documentation", SeverityLevel.Information, properties);
            throw new IllegalArgumentException("Model not found");
        }

        return documentationRepository.findByModel_ModelIdAndActiveTrue(modelId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get specific documentation by ID
     * 
     * @param docId Documentation ID
     * @return Documentation response
     */
    @Transactional(readOnly = true)
    public ApiDocumentationResponse getDocumentationById(Integer docId) {
        ApiDocumentation documentation = documentationRepository.findByDocIdAndActiveTrue(docId)
                .orElseThrow(() -> {
                    Map<String, String> properties = new HashMap<>();
                    properties.put("docId", String.valueOf(docId));
                    telemetryClient.trackTrace("Documentation not found", SeverityLevel.Information, properties);
                    return new IllegalArgumentException("Documentation not found");
                });

        return mapToResponse(documentation);
    }

    /**
     * Update documentation
     * 
     * @param docId Documentation ID
     * @param updateRequest Update request
     * @return Updated documentation response
     */
    public ApiDocumentationResponse updateDocumentation(Integer docId, UpdateApiDocumentationRequest updateRequest) {
        ApiDocumentation documentation = documentationRepository.findByDocIdAndActiveTrue(docId)
                .orElseThrow(() -> {
                    Map<String, String> properties = new HashMap<>();
                    properties.put("docId", String.valueOf(docId));
                    telemetryClient.trackTrace("Documentation not found for update", SeverityLevel.Information, properties);
                    return new IllegalArgumentException("Documentation not found");
                });

        if (updateRequest.getTitle() != null && !updateRequest.getTitle().isBlank()) {
            documentation.setTitle(updateRequest.getTitle());
        }
        if (updateRequest.getContent() != null && !updateRequest.getContent().isBlank()) {
            documentation.setContent(updateRequest.getContent());
        }

        documentation.setUpdatedAt(LocalDateTime.now());
        ApiDocumentation updatedDoc = documentationRepository.save(documentation);
        
        Map<String, String> properties = new HashMap<>();
        properties.put("docId", String.valueOf(docId));
        telemetryClient.trackEvent("DocumentationUpdated", properties, null);

        return mapToResponse(updatedDoc);
    }

    /**
     * Delete documentation
     * 
     * @param docId Documentation ID
     */
    public void deleteDocumentation(Integer docId) {
        ApiDocumentation documentation = documentationRepository.findByDocIdAndActiveTrue(docId)
                .orElseThrow(() -> {
                    Map<String, String> properties = new HashMap<>();
                    properties.put("docId", String.valueOf(docId));
                    telemetryClient.trackTrace("Documentation not found for delete", SeverityLevel.Information, properties);
                    return new IllegalArgumentException("Documentation not found");
                });

        documentation.setActive(false);
        documentation.setUpdatedAt(LocalDateTime.now());
        documentationRepository.save(documentation);
        
        Map<String, String> properties = new HashMap<>();
        properties.put("docId", String.valueOf(docId));
        telemetryClient.trackEvent("DocumentationSoftDeleted", properties, null);
    }

    /**
     * Map documentation entity to response DTO
     * 
     * @param documentation Documentation entity
     * @return Documentation response
     */
    private ApiDocumentationResponse mapToResponse(ApiDocumentation documentation) {
        return ApiDocumentationResponse.builder()
                .docId(documentation.getDocId())
                .title(documentation.getTitle())
                .content(documentation.getContent())
                .modelId(documentation.getModel().getModelId())
                .modelName(documentation.getModel().getFullname())
                .createdAt(documentation.getCreatedAt())
                .updatedAt(documentation.getUpdatedAt())
                .build();
    }
}
