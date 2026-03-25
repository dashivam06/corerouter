package com.fleebug.corerouter.controller.token;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.token.request.CreateServiceTokenRequest;
import com.fleebug.corerouter.dto.token.response.CreateServiceTokenResponse;
import com.fleebug.corerouter.dto.token.response.ServiceTokenResponse;
import com.fleebug.corerouter.entity.token.ServiceToken;
import com.fleebug.corerouter.service.token.ServiceTokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/service-tokens")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Service Tokens", description = "Admin-only management of worker/internal service tokens")
public class ServiceTokenManagementController {

    private final ServiceTokenService serviceTokenService;
    private final TelemetryClient telemetryClient;

    @Operation(summary = "Create service token", description = "Create a new service token and return the raw token once")
    @PostMapping
    public ResponseEntity<ApiResponse<CreateServiceTokenResponse>> createToken(
            @Valid @RequestBody CreateServiceTokenRequest request,
            HttpServletRequest httpRequest) {

        Map<String, String> properties = new HashMap<>();
        properties.put("tokenName", request.getName());
        properties.put("role", request.getRole().name());
        telemetryClient.trackEvent("ServiceTokenCreation", properties, null);

        String rawToken = serviceTokenService.createToken(request.getName(), request.getRole());
        ServiceToken token = serviceTokenService.getByName(request.getName());

        CreateServiceTokenResponse data = CreateServiceTokenResponse.builder()
                .id(token.getId())
                .tokenId(token.getTokenId())
                .name(token.getName())
                .role(token.getRole())
                .active(token.isActive())
                .createdAt(token.getCreatedAt())
                .lastUsedAt(token.getLastUsedAt())
                .rawToken(rawToken)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Service token created successfully", data, httpRequest));
    }

    @Operation(summary = "List service tokens", description = "List all service tokens (metadata only)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceTokenResponse>>> listTokens(HttpServletRequest httpRequest) {
        telemetryClient.trackTrace("List service tokens request", SeverityLevel.Verbose, null);
        List<ServiceTokenResponse> data = serviceTokenService.listAll().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Service tokens retrieved successfully", data, httpRequest));
    }

    @Operation(summary = "Get service token", description = "Get service token metadata by tokenId")
    @GetMapping("/{tokenId}")
    public ResponseEntity<ApiResponse<ServiceTokenResponse>> getToken(
            @Parameter(description = "Token ID", example = "a1b2c3d4e5f6") @PathVariable String tokenId,
            HttpServletRequest httpRequest) {

        Map<String, String> properties = new HashMap<>();
        properties.put("tokenId", tokenId);
        telemetryClient.trackTrace("Get service token request", SeverityLevel.Verbose, properties);

        ServiceToken token = serviceTokenService.getByTokenId(tokenId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Service token retrieved successfully", toResponse(token), httpRequest));
    }

    @Operation(summary = "Revoke service token", description = "Deactivate a service token by tokenId")
    @PatchMapping("/{tokenId}/revoke")
    public ResponseEntity<ApiResponse<Void>> revokeToken(
            @Parameter(description = "Token ID", example = "a1b2c3d4e5f6") @PathVariable String tokenId,
            HttpServletRequest httpRequest) {

        Map<String, String> properties = new HashMap<>();
        properties.put("tokenId", tokenId);
        telemetryClient.trackEvent("ServiceTokenRevocation", properties, null);

        serviceTokenService.revokeTokenByTokenId(tokenId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Service token revoked successfully", null, httpRequest));
    }

    @Operation(summary = "Activate service token", description = "Reactivate a service token by tokenId")
    @PatchMapping("/{tokenId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateToken(
            @Parameter(description = "Token ID", example = "a1b2c3d4e5f6") @PathVariable String tokenId,
            HttpServletRequest httpRequest) {

        Map<String, String> properties = new HashMap<>();
        properties.put("tokenId", tokenId);
        telemetryClient.trackEvent("ServiceTokenActivation", properties, null);

        serviceTokenService.activateTokenByTokenId(tokenId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Service token activated successfully", null, httpRequest));
    }

    @Operation(summary = "Delete service token", description = "Permanently delete a service token by tokenId")
    @DeleteMapping("/{tokenId}")
    public ResponseEntity<ApiResponse<Void>> deleteToken(
            @Parameter(description = "Token ID", example = "a1b2c3d4e5f6") @PathVariable String tokenId,
            HttpServletRequest httpRequest) {

        Map<String, String> properties = new HashMap<>();
        properties.put("tokenId", tokenId);
        telemetryClient.trackEvent("ServiceTokenDeletion", properties, null);

        serviceTokenService.deleteTokenByTokenId(tokenId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Service token deleted successfully", null, httpRequest));
    }

    private ServiceTokenResponse toResponse(ServiceToken token) {
        return ServiceTokenResponse.builder()
                .id(token.getId())
                .tokenId(token.getTokenId())
                .name(token.getName())
                .role(token.getRole())
                .active(token.isActive())
                .createdAt(token.getCreatedAt())
                .lastUsedAt(token.getLastUsedAt())
                .build();
    }
}
