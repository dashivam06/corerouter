package com.fleebug.corerouter.controller.documentation;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.documentation.request.CreateApiDocumentationRequest;
import com.fleebug.corerouter.dto.documentation.request.UpdateApiDocumentationRequest;
import com.fleebug.corerouter.dto.documentation.response.ApiDocumentationResponse;
import com.fleebug.corerouter.service.documentation.ApiDocumentationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;


/**
 * Admin documentation management endpoints
 */
@RestController
@RequestMapping("/api/v1/admin/documentation")
@RequiredArgsConstructor
@Slf4j
public class AdminDocumentationController {

    private final ApiDocumentationService documentationService;

    @PostMapping("/models/{modelId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ApiDocumentationResponse>> createDocumentation(
            @PathVariable Integer modelId,
            @Valid @RequestBody CreateApiDocumentationRequest request,
            HttpServletRequest httpRequest) {
        log.info("Admin creating documentation for model ID: {}", modelId);
        
        ApiDocumentationResponse response = documentationService.createDocumentation(modelId, request);
        
        ApiResponse<ApiDocumentationResponse> apiResponse = ApiResponse.<ApiDocumentationResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CREATED.value())
                .success(true)
                .message("Documentation created successfully")
                .path(httpRequest.getRequestURI())
                .method(httpRequest.getMethod())
                .data(response)
                .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PutMapping("/{docId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ApiDocumentationResponse>> updateDocumentation(
            @PathVariable Integer docId,
            @Valid @RequestBody UpdateApiDocumentationRequest request,
            HttpServletRequest httpRequest) {
        log.info("Admin updating documentation with ID: {}", docId);
        
        ApiDocumentationResponse response = documentationService.updateDocumentation(docId, request);
        
        ApiResponse<ApiDocumentationResponse> apiResponse = ApiResponse.<ApiDocumentationResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .success(true)
                .message("Documentation updated successfully")
                .path(httpRequest.getRequestURI())
                .method(httpRequest.getMethod())
                .data(response)
                .build();
        
        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping("/{docId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDocumentation(
            @PathVariable Integer docId,
            HttpServletRequest httpRequest) {
        log.info("Admin deleting documentation with ID: {}", docId);
        
        documentationService.deleteDocumentation(docId);
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .success(true)
                .message("Documentation deleted successfully")
                .path(httpRequest.getRequestURI())
                .method(httpRequest.getMethod())
                .data(null)
                .build();
        
        return ResponseEntity.ok(response);
    }
}
