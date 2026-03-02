

package com.fleebug.corerouter.controller.task;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.task.request.TaskCreateRequest;
import com.fleebug.corerouter.dto.task.request.TaskStatusUpdateRequest;
import com.fleebug.corerouter.dto.task.response.TaskAsyncResponse;
import com.fleebug.corerouter.dto.task.response.TaskStatusResponse;
import com.fleebug.corerouter.entity.task.Task;
import com.fleebug.corerouter.service.task.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/v1/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;
    
    private final ObjectMapper objectMapper ;

    @PostMapping
    public ResponseEntity<ApiResponse<TaskAsyncResponse>> createTask(
            @Valid @RequestBody TaskCreateRequest request,
            HttpServletRequest httpRequest) {

        log.info("Received task creation request for modelId={}, apiKeyId={}",
                request.getModelId(), request.getApiKeyId());

        Task task = taskService.createTask(request);

        TaskAsyncResponse response = TaskAsyncResponse.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus())
                .build();

        ApiResponse<TaskAsyncResponse> apiResponse = ApiResponse.<TaskAsyncResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.ACCEPTED.value())
                .success(true)
                .message("Task enqueued successfully")
                .path(httpRequest.getRequestURI())
                .method(httpRequest.getMethod())
                .data(response)
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(apiResponse);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskStatusResponse>> getTask(
            @PathVariable String taskId,
            HttpServletRequest httpRequest) {

        log.info("Received task status request - taskId={}", taskId);

        Task task = taskService.getTaskById(taskId);

        Object resultObject = null;
        if (task.getResultPayload() != null) {
            try {
                resultObject = objectMapper.readValue(task.getResultPayload(), Object.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize task result. Returning raw string. taskId={}", taskId, e);
                resultObject = task.getResultPayload();
            }
        }

        TaskStatusResponse taskStatusResponse = TaskStatusResponse.builder()
                .status(task.getStatus())
                .result(resultObject)
                .build();

        ApiResponse<TaskStatusResponse> apiResponse = ApiResponse.<TaskStatusResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .success(true)
                .message("Task status retrieved successfully")
                .path(httpRequest.getRequestURI())
                .method(httpRequest.getMethod())
                .data(taskStatusResponse)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @PatchMapping("/status")
    public ResponseEntity<ApiResponse<TaskStatusResponse>> updateTaskStatus(
            @Valid @RequestBody TaskStatusUpdateRequest request,
            HttpServletRequest httpRequest) {

        log.info("Received task status update - taskId={}, status={}", request.getTaskId(), request.getStatus());

        Task task = taskService.updateTaskStatus(request);

        Object resultObject = null;
        if (task.getResultPayload() != null) {
            try {
                resultObject = objectMapper.readValue(task.getResultPayload(), Object.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize task result after update. Returning raw string. taskId={}", request.getTaskId(), e);
                resultObject = task.getResultPayload();
            }
        }

        TaskStatusResponse taskStatusResponse = TaskStatusResponse.builder()
                .status(task.getStatus())
                .result(resultObject)
                .build();

        ApiResponse<TaskStatusResponse> apiResponse = ApiResponse.<TaskStatusResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .success(true)
                .message("Task status updated successfully")
                .path(httpRequest.getRequestURI())
                .method(httpRequest.getMethod())
                .data(taskStatusResponse)
                .build();

        return ResponseEntity.ok(apiResponse);
    }
}

