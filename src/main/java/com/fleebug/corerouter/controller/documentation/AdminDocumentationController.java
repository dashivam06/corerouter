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

    /**
     * Create documentation for a model (Admin only)
     * 
     * @param modelId Model ID
     * @param request Create documentation request
     * @param httpRequest HttpServletRequest
     * @return Created documentation
     */
    @PostMapping("/models/{modelId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ApiDocumentationResponse>> createDocumentation(
            @PathVariable Integer modelId,
            @Valid @RequestBody CreateApiDocumentationRequest request,
            HttpServletRequest httpRequest) {
        try {
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
        } catch (IllegalArgumentException e) {
            log.error("Error creating documentation: {}", e.getMessage());
            ApiResponse<ApiDocumentationResponse> response = ApiResponse.<ApiDocumentationResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.NOT_FOUND.value())
                    .success(false)
                    .message(e.getMessage())
                    .path(httpRequest.getRequestURI())
                    .method(httpRequest.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Unexpected error creating documentation", e);
            ApiResponse<ApiDocumentationResponse> response = ApiResponse.<ApiDocumentationResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .success(false)
                    .message("An unexpected error occurred")
                    .path(httpRequest.getRequestURI())
                    .method(httpRequest.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Update documentation (Admin only)
     * 
     * @param docId Documentation ID
     * @param request Update documentation request
     * @param httpRequest HttpServletRequest
     * @return Updated documentation
     */
    @PutMapping("/{docId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ApiDocumentationResponse>> updateDocumentation(
            @PathVariable Integer docId,
            @Valid @RequestBody UpdateApiDocumentationRequest request,
            HttpServletRequest httpRequest) {
        try {
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
        } catch (IllegalArgumentException e) {
            log.error("Error updating documentation: {}", e.getMessage());
            ApiResponse<ApiDocumentationResponse> response = ApiResponse.<ApiDocumentationResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.NOT_FOUND.value())
                    .success(false)
                    .message(e.getMessage())
                    .path(httpRequest.getRequestURI())
                    .method(httpRequest.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Unexpected error updating documentation", e);
            ApiResponse<ApiDocumentationResponse> response = ApiResponse.<ApiDocumentationResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .success(false)
                    .message("An unexpected error occurred")
                    .path(httpRequest.getRequestURI())
                    .method(httpRequest.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Delete documentation (Admin only)
     * 
     * @param docId Documentation ID
     * @param httpRequest HttpServletRequest
     * @return Empty response
     */
    @DeleteMapping("/{docId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDocumentation(
            @PathVariable Integer docId,
            HttpServletRequest httpRequest) {
        try {
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
        } catch (IllegalArgumentException e) {
            log.error("Error deleting documentation: {}", e.getMessage());
            ApiResponse<Void> response = ApiResponse.<Void>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.NOT_FOUND.value())
                    .success(false)
                    .message(e.getMessage())
                    .path(httpRequest.getRequestURI())
                    .method(httpRequest.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Unexpected error deleting documentation", e);
            ApiResponse<Void> response = ApiResponse.<Void>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .success(false)
                    .message("An unexpected error occurred")
                    .path(httpRequest.getRequestURI())
                    .method(httpRequest.getMethod())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
