package com.fleebug.corerouter.controller.billing;

import com.fleebug.corerouter.dto.billing.request.CreateBillingConfigRequest;
import com.fleebug.corerouter.dto.billing.request.RecordUsageRequest;
import com.fleebug.corerouter.dto.billing.request.UpdateBillingConfigRequest;
import com.fleebug.corerouter.dto.billing.response.BillingConfigResponse;
import com.fleebug.corerouter.dto.billing.response.UsageRecordResponse;
import com.fleebug.corerouter.dto.billing.response.UsageSummaryResponse;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.service.billing.BillingConfigService;
import com.fleebug.corerouter.service.billing.UsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/billing")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Billing", description = "Billing configuration and usage management (ADMIN only)")
public class AdminBillingController {

    private final BillingConfigService billingConfigService;
    private final UsageService usageService;

    // ---- Billing Config CRUD ----

    /**
     * Create a new billing configuration for a model.
     *
     * @param createRequest billing config creation payload
     * @param request       HTTP servlet request
     * @return created billing config
     */
    @Operation(summary = "Create billing config", description = "Create a new billing configuration for a model with pricing type and metadata")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Billing config created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or billing config already exists for model"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Model not found")
    })
    @PostMapping("/configs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BillingConfigResponse>> createBillingConfig(
            @Valid @RequestBody CreateBillingConfigRequest createRequest,
            HttpServletRequest request) {
        log.info("Create billing config request for modelId={}", createRequest.getModelId());

        BillingConfigResponse response = billingConfigService.createBillingConfig(createRequest);

        ApiResponse<BillingConfigResponse> apiResponse = ApiResponse.<BillingConfigResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CREATED.value())
                .success(true)
                .message("Billing config created successfully")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(response)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    /**
     * Get all billing configurations.
     *
     * @param request HTTP servlet request
     * @return list of all billing configs
     */
    @Operation(summary = "Get all billing configs", description = "Retrieve all billing configurations across all models")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Billing configs retrieved successfully")
    })
    @GetMapping("/configs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<BillingConfigResponse>>> getAllBillingConfigs(
            HttpServletRequest request) {
        log.info("Get all billing configs request received");

        List<BillingConfigResponse> configs = billingConfigService.getAllBillingConfigs();

        ApiResponse<List<BillingConfigResponse>> apiResponse = ApiResponse.<List<BillingConfigResponse>>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .success(true)
                .message("Billing configs retrieved successfully")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(configs)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Get a billing configuration by its ID.
     *
     * @param billingId billing config ID
     * @param request   HTTP servlet request
     * @return billing config
     */
    @Operation(summary = "Get billing config by ID", description = "Retrieve a billing configuration by its ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Billing config retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Billing config not found")
    })
    @GetMapping("/configs/{billingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BillingConfigResponse>> getBillingConfigById(
            @Parameter(description = "Billing config ID", example = "1") @PathVariable Integer billingId,
            HttpServletRequest request) {
        log.info("Get billing config by ID={}", billingId);

        BillingConfigResponse response = billingConfigService.getBillingConfigById(billingId);

        ApiResponse<BillingConfigResponse> apiResponse = ApiResponse.<BillingConfigResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .success(true)
                .message("Billing config retrieved successfully")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(response)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Get a billing configuration by model ID.
     *
     * @param modelId model ID
     * @param request HTTP servlet request
     * @return billing config for the model
     */
    @Operation(summary = "Get billing config by model", description = "Retrieve the billing configuration associated with a specific model")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Billing config retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Billing config not found for model")
    })
    @GetMapping("/configs/model/{modelId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BillingConfigResponse>> getBillingConfigByModelId(
            @Parameter(description = "Model ID", example = "5") @PathVariable Integer modelId,
            HttpServletRequest request) {
        log.info("Get billing config for modelId={}", modelId);

        BillingConfigResponse response = billingConfigService.getBillingConfigByModelId(modelId);

        ApiResponse<BillingConfigResponse> apiResponse = ApiResponse.<BillingConfigResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .success(true)
                .message("Billing config retrieved successfully")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(response)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Update an existing billing configuration.
     *
     * @param billingId     billing config ID
     * @param updateRequest update payload
     * @param request       HTTP servlet request
     * @return updated billing config
     */
    @Operation(summary = "Update billing config", description = "Update pricing type and/or metadata of an existing billing configuration")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Billing config updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Billing config not found")
    })
    @PutMapping("/configs/{billingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BillingConfigResponse>> updateBillingConfig(
            @Parameter(description = "Billing config ID", example = "1") @PathVariable Integer billingId,
            @Valid @RequestBody UpdateBillingConfigRequest updateRequest,
            HttpServletRequest request) {
        log.info("Update billing config ID={}", billingId);

        BillingConfigResponse response = billingConfigService.updateBillingConfig(billingId, updateRequest);

        ApiResponse<BillingConfigResponse> apiResponse = ApiResponse.<BillingConfigResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .success(true)
                .message("Billing config updated successfully")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(response)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Delete a billing configuration.
     *
     * @param billingId billing config ID
     * @param request   HTTP servlet request
     * @return empty response
     */
    @Operation(summary = "Delete billing config", description = "Permanently delete a billing configuration")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Billing config deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Billing config not found")
    })
    @DeleteMapping("/configs/{billingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBillingConfig(
            @Parameter(description = "Billing config ID", example = "1") @PathVariable Integer billingId,
            HttpServletRequest request) {
        log.info("Delete billing config ID={}", billingId);

        billingConfigService.deleteBillingConfig(billingId);

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .success(true)
                .message("Billing config deleted successfully")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(null)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    // ---- Usage Recording (called internally or by admin) ----

    /**
     * Record usage for a completed task.
     *
     * @param recordRequest usage recording payload
     * @param request       HTTP servlet request
     * @return recorded usage
     */
    @Operation(summary = "Record usage", description = "Record usage for a completed task. A task can have multiple usage records (e.g. input tokens + output tokens).")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Usage recorded successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or duplicate unit type for task"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Task not found or billing config missing")
    })
    @PostMapping("/usage")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UsageRecordResponse>> recordUsage(
            @Valid @RequestBody RecordUsageRequest recordRequest,
            HttpServletRequest request) {
        log.info("Record usage request for taskId={}", recordRequest.getTaskId());

        UsageRecordResponse response = usageService.recordUsage(recordRequest);

        ApiResponse<UsageRecordResponse> apiResponse = ApiResponse.<UsageRecordResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CREATED.value())
                .success(true)
                .message("Usage recorded successfully")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(response)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    // ---- Usage Queries (admin) ----

    /**
     * Get all usage records for a task.
     *
     * @param taskId  task ID
     * @param request HTTP servlet request
     * @return list of usage records
     */
    @Operation(summary = "Get usage by task", description = "Retrieve all usage records for a specific task")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usage records retrieved successfully")
    })
    @GetMapping("/usage/task/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UsageRecordResponse>>> getUsageByTask(
            @Parameter(description = "Task ID", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479") @PathVariable String taskId,
            HttpServletRequest request) {
        log.info("Get usage for taskId={}", taskId);

        List<UsageRecordResponse> records = usageService.getUsageByTaskId(taskId);

        ApiResponse<List<UsageRecordResponse>> apiResponse = ApiResponse.<List<UsageRecordResponse>>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .success(true)
                .message("Usage records retrieved successfully")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(records)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Get usage summary for an API key grouped by unit type.
     *
     * @param apiKeyId API key ID
     * @param from     period start
     * @param to       period end
     * @param request  HTTP servlet request
     * @return usage summary with breakdown
     */
    @Operation(summary = "Usage summary by unit type", description = "Get aggregated usage summary for an API key grouped by unit type within a date range")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usage summary retrieved successfully")
    })
    @GetMapping("/usage/apikey/{apiKeyId}/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UsageSummaryResponse>> getUsageSummaryByApiKey(
            @Parameter(description = "API key ID", example = "10") @PathVariable Integer apiKeyId,
            @Parameter(description = "Period start (ISO 8601)", example = "2026-03-01T00:00:00") @RequestParam LocalDateTime from,
            @Parameter(description = "Period end (ISO 8601)", example = "2026-03-31T23:59:59") @RequestParam LocalDateTime to,
            HttpServletRequest request) {
        log.info("Get usage summary for apiKeyId={} from={} to={}", apiKeyId, from, to);

        UsageSummaryResponse response = usageService.getUsageSummaryByApiKey(apiKeyId, from, to);

        ApiResponse<UsageSummaryResponse> apiResponse = ApiResponse.<UsageSummaryResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .success(true)
                .message("Usage summary retrieved successfully")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(response)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Get usage summary for an API key grouped by model and unit type.
     *
     * @param apiKeyId API key ID
     * @param from     period start
     * @param to       period end
     * @param request  HTTP servlet request
     * @return usage summary with per-model breakdown
     */
    @Operation(summary = "Usage summary by model", description = "Get aggregated usage summary for an API key grouped by model and unit type within a date range")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usage summary retrieved successfully")
    })
    @GetMapping("/usage/apikey/{apiKeyId}/summary/by-model")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UsageSummaryResponse>> getUsageSummaryByModel(
            @Parameter(description = "API key ID", example = "10") @PathVariable Integer apiKeyId,
            @Parameter(description = "Period start (ISO 8601)", example = "2026-03-01T00:00:00") @RequestParam LocalDateTime from,
            @Parameter(description = "Period end (ISO 8601)", example = "2026-03-31T23:59:59") @RequestParam LocalDateTime to,
            HttpServletRequest request) {
        log.info("Get usage summary by model for apiKeyId={}", apiKeyId);

        UsageSummaryResponse response = usageService.getUsageSummaryByApiKeyGroupedByModel(apiKeyId, from, to);

        ApiResponse<UsageSummaryResponse> apiResponse = ApiResponse.<UsageSummaryResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .success(true)
                .message("Usage summary by model retrieved successfully")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(response)
                .build();

        return ResponseEntity.ok(apiResponse);
    }
}
