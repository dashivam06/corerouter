package com.fleebug.corerouter.controller.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.llm.request.ChatCompletionRequest;
import com.fleebug.corerouter.dto.task.request.TaskCreateRequest;
import com.fleebug.corerouter.dto.task.response.TaskAsyncResponse;
import com.fleebug.corerouter.entity.model.Model;
import com.fleebug.corerouter.entity.task.Task;
import com.fleebug.corerouter.exception.model.ModelNotFoundException;
import com.fleebug.corerouter.repository.model.ModelRepository;
import com.fleebug.corerouter.service.task.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
public class LlmController {

    private static final String API_KEY_ID_HEADER = "X-API-Key-ID";

    private final TaskService taskService;
    private final ModelRepository modelRepository;
    private final ObjectMapper objectMapper;

    @PostMapping("/completions")
    public ResponseEntity<ApiResponse<TaskAsyncResponse>> chatCompletions(
            @Valid @RequestBody ChatCompletionRequest chatRequest,
            HttpServletRequest request) throws JsonProcessingException {
        log.info("Chat completion request - model: {}", chatRequest.getModel());

        Integer apiKeyId = requireApiKeyId(request);
        Model model = modelRepository.findByUsername(chatRequest.getModel())
                .orElseThrow(() -> new ModelNotFoundException("username", chatRequest.getModel()));

        Map<String, Object> payload = objectMapper.convertValue(chatRequest, new TypeReference<>() {});
        TaskCreateRequest taskRequest = TaskCreateRequest.builder()
                .apiKeyId(apiKeyId)
                .modelId(model.getModelId())
                .payload(payload)
                .build();

        Task task = taskService.createTask(taskRequest);

        TaskAsyncResponse data = TaskAsyncResponse.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus())
                .build();
        ApiResponse<TaskAsyncResponse> apiResponse = ApiResponse.<TaskAsyncResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.ACCEPTED.value())
                .success(true)
                .message("Task enqueued successfully")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .data(data)
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(apiResponse);
    }

    private static Integer requireApiKeyId(HttpServletRequest request) {
        String value = request.getHeader(API_KEY_ID_HEADER);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(API_KEY_ID_HEADER + " header is required");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(API_KEY_ID_HEADER + " must be a valid integer");
        }
    }
}
