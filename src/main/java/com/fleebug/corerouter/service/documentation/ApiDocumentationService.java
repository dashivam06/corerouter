package com.fleebug.corerouter.service.documentation;

import com.fleebug.corerouter.dto.documentation.request.CreateApiDocumentationRequest;
import com.fleebug.corerouter.dto.documentation.request.UpdateApiDocumentationRequest;
import com.fleebug.corerouter.dto.documentation.response.ApiDocumentationResponse;
import com.fleebug.corerouter.entity.documentation.ApiDocumentation;
import com.fleebug.corerouter.entity.model.Model;
import com.fleebug.corerouter.repository.documentation.ApiDocumentationRepository;
import com.fleebug.corerouter.repository.model.ModelRepository;
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
public class ApiDocumentationService {

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
        log.info("Creating documentation for model ID: {}", modelId);

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> {
                    log.warn("Model not found with ID: {}", modelId);
                    return new IllegalArgumentException("Model not found");
                });

        ApiDocumentation documentation = ApiDocumentation.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .model(model)
                .createdAt(LocalDateTime.now())
                .build();

        ApiDocumentation savedDoc = documentationRepository.save(documentation);
        log.info("Documentation created successfully with ID: {}", savedDoc.getDocId());

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
        log.info("Fetching documentation for model ID: {}", modelId);

        // Verify model exists
        if (!modelRepository.existsById(modelId)) {
            log.warn("Model not found with ID: {}", modelId);
            throw new IllegalArgumentException("Model not found");
        }

        return documentationRepository.findByModel_ModelId(modelId)
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
        log.info("Fetching documentation with ID: {}", docId);

        ApiDocumentation documentation = documentationRepository.findById(docId)
                .orElseThrow(() -> {
                    log.warn("Documentation not found with ID: {}", docId);
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
        log.info("Updating documentation with ID: {}", docId);

        ApiDocumentation documentation = documentationRepository.findById(docId)
                .orElseThrow(() -> {
                    log.warn("Documentation not found with ID: {}", docId);
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
        log.info("Documentation updated successfully with ID: {}", docId);

        return mapToResponse(updatedDoc);
    }

    /**
     * Delete documentation
     * 
     * @param docId Documentation ID
     */
    public void deleteDocumentation(Integer docId) {
        log.info("Deleting documentation with ID: {}", docId);

        ApiDocumentation documentation = documentationRepository.findById(docId)
                .orElseThrow(() -> {
                    log.warn("Documentation not found with ID: {}", docId);
                    return new IllegalArgumentException("Documentation not found");
                });

        documentationRepository.deleteById(docId);
        log.info("Documentation deleted successfully with ID: {}", docId);
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
