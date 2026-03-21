package com.fleebug.corerouter.service.task;


import com.fleebug.corerouter.dto.task.request.TaskCreateRequest;
import com.fleebug.corerouter.dto.task.request.TaskStatusUpdateRequest;
import com.fleebug.corerouter.entity.apikey.ApiKey;
import com.fleebug.corerouter.entity.model.Model;
import com.fleebug.corerouter.entity.task.Task;
import com.fleebug.corerouter.enums.apikey.ApiKeyStatus;
import com.fleebug.corerouter.enums.task.TaskStatus;
import com.fleebug.corerouter.exception.apikey.ApiKeyNotFoundException;
import com.fleebug.corerouter.exception.apikey.ApiKeyRevokedException;
import com.fleebug.corerouter.exception.model.ModelNotFoundException;
import com.fleebug.corerouter.exception.task.TaskNotFoundException;
import com.fleebug.corerouter.exception.task.TaskPayloadInvalidException;
import com.fleebug.corerouter.repository.apikey.ApiKeyRepository;
import com.fleebug.corerouter.repository.model.ModelRepository;
import com.fleebug.corerouter.repository.task.TaskRepository;
import com.fleebug.corerouter.service.redis.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private static final String TASK_STREAM_KEY = "stream:tasks";

    private final TaskRepository taskRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ModelRepository modelRepository;
    private final RedisService redisService;
    private final ObjectMapper objectMapper ;

    @Transactional
    public Task createTask(TaskCreateRequest request) {
        String taskId = UUID.randomUUID().toString();
        
        String payloadJson;
        
        try {
            payloadJson = objectMapper.writeValueAsString(request.getPayload());
        } catch (JsonProcessingException e) {
            throw new TaskPayloadInvalidException("Failed to serialize task payload", e);
        }

        ApiKey apiKey = apiKeyRepository.findById(request.getApiKeyId())
                .orElseThrow(() -> new ApiKeyNotFoundException(request.getApiKeyId()));

        if (apiKey.getStatus() != ApiKeyStatus.ACTIVE) {
            throw new ApiKeyRevokedException("API key is not active (status: " + apiKey.getStatus() + ")");
        }

        Model model = modelRepository.findById(request.getModelId())
                .orElseThrow(() -> new ModelNotFoundException(request.getModelId()));

        Task task = Task.builder()
                .taskId(taskId)
                .apiKey(apiKey)
                .model(model)
                .requestPayload(payloadJson)
                .status(TaskStatus.QUEUED)
                .createdAt(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
                .build();

        Task saved = taskRepository.save(task);
        redisService.appendToStream(
                TASK_STREAM_KEY,
                Map.of(
                        "taskId", saved.getTaskId(),
                        "apiKeyId", saved.getApiKey().getApiKeyId().toString(),
                        "modelId", saved.getModel().getModelId().toString(),
                        "payload", saved.getRequestPayload(),
                        "timestamp", LocalDateTime.now().toString()
                )
        );
        log.info("Task created and pushed to stream - taskId={}", saved.getTaskId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Task getTaskById(String taskId) {
        return taskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    @Transactional
    public Task updateTaskStatus(TaskStatusUpdateRequest request) {
        Task task = getTaskById(request.getTaskId());
        task.setStatus(request.getStatus());
        task.setUpdatedAt(LocalDateTime.now());
        task.setUsageMetadata(request.getUsageMetadata());

        if (request.getResult() != null) {
            try {
                task.setResultPayload(objectMapper.writeValueAsString(request.getResult()));
            } catch (JsonProcessingException e) {
                throw new TaskPayloadInvalidException("Failed to serialize task result", e);
            }
        }

        if (request.getStatus() == TaskStatus.COMPLETED || request.getStatus() == TaskStatus.FAILED) {
            task.setCompletedAt(LocalDateTime.now());
            long processingTimeMs = java.time.Duration.between(task.getCreatedAt(), task.getCompletedAt()).toMillis();
            task.setProcessingTimeMs(processingTimeMs);
        }

        Task saved = taskRepository.save(task);
        log.info("Task status updated - taskId={}, status={}", saved.getTaskId(), saved.getStatus());
        return saved;
    }
}

