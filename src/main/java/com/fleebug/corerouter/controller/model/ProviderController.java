package com.fleebug.corerouter.controller.model;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.model.request.CreateProviderRequest;
import com.fleebug.corerouter.dto.model.request.UpdateProviderRequest;
import com.fleebug.corerouter.dto.model.response.ProviderResponse;
import com.fleebug.corerouter.enums.activity.ActivityAction;
import com.fleebug.corerouter.enums.model.ProviderStatus;
import com.fleebug.corerouter.security.details.CustomUserDetails;
import com.fleebug.corerouter.service.activity.ActivityLogService;
import com.fleebug.corerouter.service.model.ProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
@Tag(name = "Providers", description = "AI model provider management — create, view, update, and manage providers")
public class ProviderController {

    private final ProviderService providerService;
    private final ActivityLogService activityLogService;

    /**
     * Create a new provider
     */
    @Operation(summary = "Create provider", description = "Create a new AI model provider with name, country, and company information")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Provider created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProviderResponse>> createProvider(
            @Valid @RequestBody CreateProviderRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {

        ProviderResponse provider = providerService.createProvider(request);
        activityLogService.log(
            ((CustomUserDetails) authentication.getPrincipal()).getUser(),
            ActivityAction.ADMIN_PROVIDER_CREATED,
            "A provider was created: " + provider.getProviderName() + ".",
            servletRequest.getRemoteAddr()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Provider created successfully", provider, servletRequest));
    }

    /**
     * Get provider by ID
     */
    @Operation(summary = "Get provider by ID", description = "Retrieve provider details by provider ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Provider retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Provider not found")
    })
    @GetMapping("/{providerId}")
    public ResponseEntity<ApiResponse<ProviderResponse>> getProviderById(
            @Parameter(description = "Provider ID", example = "1") @PathVariable Integer providerId,
            HttpServletRequest request) {

        ProviderResponse provider = providerService.getProviderById(providerId);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Provider retrieved successfully", provider, request));
    }

    /**
     * Get provider by name
     */
    @Operation(summary = "Get provider by name", description = "Retrieve provider details by provider name")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Provider retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Provider not found")
    })
    @GetMapping("/name/{providerName}")
    public ResponseEntity<ApiResponse<ProviderResponse>> getProviderByName(
            @Parameter(description = "Provider name", example = "Mistral AI") @PathVariable String providerName,
            HttpServletRequest request) {

        ProviderResponse provider = providerService.getProviderByName(providerName);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Provider retrieved successfully", provider, request));
    }

    /**
     * Get all providers
     */
    @Operation(summary = "List all providers", description = "Retrieve a list of all providers in the system")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Providers retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProviderResponse>>> getAllProviders(HttpServletRequest request) {

        List<ProviderResponse> providers = providerService.getAllProviders();

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Providers retrieved successfully", providers, request));
    }

    /**
     * Get all active providers
     */
    @Operation(summary = "List active providers", description = "Retrieve a list of all active providers (status = ACTIVE)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active providers retrieved successfully")
    })
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ProviderResponse>>> getActiveProviders(HttpServletRequest request) {

        List<ProviderResponse> providers = providerService.getActiveProviders();

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Active providers retrieved successfully", providers, request));
    }

    /**
     * Update provider
     */
    @Operation(summary = "Update provider", description = "Update provider information like name, country, company, or logo")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Provider updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Provider not found")
    })
    @PutMapping("/{providerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProviderResponse>> updateProvider(
            @Parameter(description = "Provider ID", example = "1") @PathVariable Integer providerId,
            @Valid @RequestBody UpdateProviderRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {

        ProviderResponse provider = providerService.updateProvider(providerId, request);
        activityLogService.log(
                ((CustomUserDetails) authentication.getPrincipal()).getUser(),
                ActivityAction.ADMIN_PROVIDER_UPDATED,
            "A provider was updated.",
                servletRequest.getRemoteAddr()
        );

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Provider updated successfully", provider, servletRequest));
    }

    /**
     * Change provider status
     */
    @Operation(summary = "Change provider status", description = "Change provider status (ACTIVE, DISABLED, SUSPENDED, DELETED). Changing status affects all linked models.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Provider status changed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Provider not found")
    })
    @PatchMapping("/{providerId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProviderResponse>> changeProviderStatus(
            @Parameter(description = "Provider ID", example = "1") @PathVariable Integer providerId,
            @Parameter(description = "New status", example = "ACTIVE") @RequestParam ProviderStatus status,
            Authentication authentication,
            HttpServletRequest request) {

        ProviderResponse provider = providerService.changeProviderStatus(providerId, status);
        activityLogService.log(
                ((CustomUserDetails) authentication.getPrincipal()).getUser(),
                ActivityAction.ADMIN_PROVIDER_STATUS_CHANGED,
            "Provider status was changed to " + status + ".",
                request.getRemoteAddr()
        );

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Provider status changed successfully", provider, request));
    }

    /**
     * Delete provider (soft delete)
     */
    @Operation(summary = "Delete provider", description = "Soft delete a provider by marking its status as DELETED")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Provider deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Provider not found")
    })
    @DeleteMapping("/{providerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProvider(
            @Parameter(description = "Provider ID", example = "1") @PathVariable Integer providerId,
            Authentication authentication,
            HttpServletRequest request) {

        providerService.deleteProvider(providerId);
        activityLogService.log(
                ((CustomUserDetails) authentication.getPrincipal()).getUser(),
                ActivityAction.ADMIN_PROVIDER_DELETED,
            "A provider was deleted.",
                request.getRemoteAddr()
        );

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Provider deleted successfully", null, request));
    }
}
