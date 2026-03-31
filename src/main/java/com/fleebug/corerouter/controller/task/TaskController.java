

package com.fleebug.corerouter.controller.task;

import com.microsoft.applicationinsights.TelemetryClient;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.task.request.TaskCreateRequest;
import com.fleebug.corerouter.dto.task.request.TaskStatusUpdateRequest;
import com.fleebug.corerouter.dto.task.response.TaskAsyncResponse;
import com.fleebug.corerouter.dto.task.response.TaskStatusResponse;
import com.fleebug.corerouter.entity.apikey.ApiKey;
import com.fleebug.corerouter.entity.task.Task;
import com.fleebug.corerouter.enums.apikey.ApiKeyStatus;
import com.fleebug.corerouter.repository.apikey.ApiKeyRepository;
import com.fleebug.corerouter.service.apikey.ApiKeyService;
import com.fleebug.corerouter.service.task.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Async task management — create, poll status, and update results")
public class TaskController {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final TaskService taskService;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyService apiKeyService;
    
    private final ObjectMapper objectMapper;
    private final TelemetryClient telemetryClient;

    /**
     * @deprecated This method is generic and works, but it is **encouraged** 
     * to use the respective chat or OCR endpoint to complete the task properly.
     * Using this directly may bypass validations or workflow-specific logic.
     */
    @Deprecated
    @Operation(summary = "Create task", description = "Enqueue a new async task for processing")
    @PostMapping
    public ResponseEntity<ApiResponse<TaskAsyncResponse>> createTask(
            @Valid @RequestBody TaskCreateRequest request,
            HttpServletRequest httpRequest) {

        Map<String, String> properties = new HashMap<>();
        properties.put("modelId", String.valueOf(request.getModelId()));
        properties.put("apiKeyId", String.valueOf(request.getApiKeyId()));
        // telemetryClient.trackTrace("Task creation request", SeverityLevel.Verbose, properties);

        Task task = taskService.createTask(request);

        TaskAsyncResponse response = TaskAsyncResponse.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus())
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(HttpStatus.ACCEPTED, "Task enqueued successfully", response, httpRequest));
    }

    @Operation(summary = "Get task status", description = "Retrieve current status and result of a task")
    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskStatusResponse>> getTask(
            @Parameter(description = "Task ID", example = "abc-123") @PathVariable String taskId,
            HttpServletRequest httpRequest) {

        Map<String, String> properties = new HashMap<>();
        properties.put("taskId", taskId);
        // telemetryClient.trackTrace("Task status request", SeverityLevel.Verbose, properties);

        ApiKey apiKey = requireApiKey(httpRequest);

        Task task = taskService.getTaskById(taskId);

        if (!task.getApiKey().getApiKeyId().equals(apiKey.getApiKeyId())) {
            throw new IllegalArgumentException("This API key does not have permission to access this task");
        }

        Object resultObject = null;
        if (task.getResultPayload() != null) {
            try {
                resultObject = objectMapper.readValue(task.getResultPayload(), Object.class);
            } catch (Exception e) {
                telemetryClient.trackException(e, properties, null);
                resultObject = task.getResultPayload();
            }
        }

        TaskStatusResponse taskStatusResponse = TaskStatusResponse.builder()
                .status(task.getStatus())
                .result(resultObject)
                .build();

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Task status retrieved successfully", taskStatusResponse, httpRequest));
    }

    @Operation(summary = "Update task status", description = "Update the status and optionally the result of a task")
    @PatchMapping("/status")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<ApiResponse<TaskStatusResponse>> updateTaskStatus(
            @Valid @RequestBody TaskStatusUpdateRequest request,
            HttpServletRequest httpRequest) {

        Map<String, String> properties = new HashMap<>();
        properties.put("taskId", request.getTaskId());
        properties.put("status", String.valueOf(request.getStatus()));
        telemetryClient.trackEvent("TaskStatusUpdate", properties, null);

        Task task = taskService.updateTaskStatus(request);

        Object resultObject = null;
        if (task.getResultPayload() != null) {
            try {
                resultObject = objectMapper.readValue(task.getResultPayload(), Object.class);
            } catch (Exception e) {
                telemetryClient.trackException(e, properties, null);
                resultObject = task.getResultPayload();
            }
        }

        TaskStatusResponse taskStatusResponse = TaskStatusResponse.builder()
                .status(task.getStatus())
                .result(resultObject)
                .build();

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Task status updated successfully", taskStatusResponse, httpRequest));
    }

    private ApiKey requireApiKey(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Missing or invalid Authorization header. Expected 'Bearer <API_KEY>'");
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        String hashedToken = apiKeyService.hashKey(token);

        ApiKey apiKey = apiKeyRepository.findByKey(hashedToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid API Key"));

        if (apiKey.getStatus() != ApiKeyStatus.ACTIVE) {
            throw new IllegalArgumentException("API Key is not active");
        }

        return apiKey;
    }
}

