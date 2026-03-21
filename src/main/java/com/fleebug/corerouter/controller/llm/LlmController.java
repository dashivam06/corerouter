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
import com.fleebug.corerouter.entity.apikey.ApiKey;
import com.fleebug.corerouter.enums.apikey.ApiKeyStatus;
import com.fleebug.corerouter.repository.apikey.ApiKeyRepository;
import com.fleebug.corerouter.repository.model.ModelRepository;
import com.fleebug.corerouter.service.apikey.ApiKeyService;
import com.fleebug.corerouter.service.task.TaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat Completions", description = "LLM chat completion endpoints")
public class LlmController {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final TaskService taskService;
    private final ModelRepository modelRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Chat completions", description = "Submit a chat completion request to the specified LLM model. Requires 'Authorization: Bearer <API_KEY>' header.")
    @PostMapping("/chat/completions")
    public ResponseEntity<ApiResponse<TaskAsyncResponse>> chatCompletions(
            @Valid @RequestBody ChatCompletionRequest chatRequest,
            HttpServletRequest request) throws JsonProcessingException {
        log.info("Chat completion request - model: {}", chatRequest.getModel());

        ApiKey apiKey = requireApiKey(request);
        Model model = modelRepository.findByFullname(chatRequest.getModel())
                .orElseThrow(() -> new ModelNotFoundException("username", chatRequest.getModel()));


        Map<String, Object> payload = objectMapper.convertValue(chatRequest, new TypeReference<>() {});
        TaskCreateRequest taskRequest = TaskCreateRequest.builder()
                .apiKeyId(apiKey.getApiKeyId())
                .modelId(model.getModelId())
                .payload(payload)
                .build();

        Task task = taskService.createTask(taskRequest);

        TaskAsyncResponse data = TaskAsyncResponse.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus())
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(HttpStatus.ACCEPTED, "Task enqueued successfully", data, request));
    }

    private ApiKey requireApiKey(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Missing or invalid Authorization header. Expected 'Bearer <API_KEY>'");
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        // Hash the token before looking it up, as we only store the hash
        String hashedToken = apiKeyService.hashKey(token);

        ApiKey apiKey = apiKeyRepository.findByKey(hashedToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid API Key"));

        if (apiKey.getStatus() != ApiKeyStatus.ACTIVE) {
            throw new IllegalArgumentException("API Key is not active");
        }
        
       
        return apiKey;
    }
}
