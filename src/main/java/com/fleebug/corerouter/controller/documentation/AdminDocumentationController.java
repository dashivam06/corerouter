package com.fleebug.corerouter.controller.documentation;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.documentation.request.CreateApiDocumentationRequest;
import com.fleebug.corerouter.dto.documentation.request.UpdateApiDocumentationRequest;
import com.fleebug.corerouter.dto.documentation.response.ApiDocumentationResponse;
import com.fleebug.corerouter.service.documentation.ApiDocumentationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


/**
 * Admin documentation management endpoints
 */
@RestController
@RequestMapping("/api/v1/admin/documentation")
@RequiredArgsConstructor
@Tag(name = "Admin Documentation", description = "Manage API documentation for AI models")
public class AdminDocumentationController {

    private final ApiDocumentationService documentationService;

    @Operation(summary = "Create documentation", description = "Create API documentation for a specific model")
    @PostMapping("/models/{modelId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ApiDocumentationResponse>> createDocumentation(
            @Parameter(description = "Model ID", example = "1") @PathVariable Integer modelId,
            @Valid @RequestBody CreateApiDocumentationRequest request,
            HttpServletRequest httpRequest) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("modelId", String.valueOf(modelId));
        // telemetryClient.trackTrace("Create documentation request", SeverityLevel.Verbose, properties);

        ApiDocumentationResponse response = documentationService.createDocumentation(modelId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Documentation created successfully", response, httpRequest));
    }

    @Operation(summary = "Update documentation", description = "Update existing API documentation")
    @PutMapping("/{docId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ApiDocumentationResponse>> updateDocumentation(
            @Parameter(description = "Documentation ID", example = "1") @PathVariable Integer docId,
            @Valid @RequestBody UpdateApiDocumentationRequest request,
            HttpServletRequest httpRequest) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("docId", String.valueOf(docId));
        // telemetryClient.trackTrace("Update documentation request", SeverityLevel.Verbose, properties);

        ApiDocumentationResponse response = documentationService.updateDocumentation(docId, request);
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Documentation updated successfully", response, httpRequest));
    }

    @Operation(summary = "Delete documentation", description = "Soft delete API documentation by its ID while keeping history")
    @DeleteMapping("/{docId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDocumentation(
            @Parameter(description = "Documentation ID", example = "1") @PathVariable Integer docId,
            HttpServletRequest httpRequest) {
        
        Map<String, String> properties = new HashMap<>();
        properties.put("docId", String.valueOf(docId));
        // telemetryClient.trackTrace("Delete documentation request", SeverityLevel.Verbose, properties);

        documentationService.deleteDocumentation(docId);
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Documentation soft deleted successfully", null, httpRequest));
    }
}
