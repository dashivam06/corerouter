package com.fleebug.corerouter.controller.ocr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.dto.ocr.request.OcrRecognitionRequest;
import com.fleebug.corerouter.dto.task.request.TaskCreateRequest;
import com.fleebug.corerouter.dto.task.response.TaskAsyncResponse;
import com.fleebug.corerouter.entity.model.Model;
import com.fleebug.corerouter.entity.task.Task;
import com.fleebug.corerouter.exception.model.ModelNotFoundException;
import com.fleebug.corerouter.repository.model.ModelRepository;
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
@RequestMapping("/api/v1/ocr")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "OCR", description = "Optical character recognition endpoints")
public class OcrController {

    private static final String API_KEY_ID_HEADER = "X-API-Key-ID";

    private final TaskService taskService;
    private final ModelRepository modelRepository;

    private final ObjectMapper objectMapper ;

    @Operation(summary = "OCR recognition", description = "Submit an OCR recognition request to the specified model")
    @PostMapping("/recognitions")
    public ResponseEntity<ApiResponse<TaskAsyncResponse>> ocrRecognition(
            @Valid @RequestBody OcrRecognitionRequest ocrRequest,
            HttpServletRequest request) throws JsonProcessingException {
        log.info("OCR recognition request - model: {}", ocrRequest.getModel());

        Integer apiKeyId = requireApiKeyId(request);
        Model model = modelRepository.findByFullname(ocrRequest.getModel())
                .orElseThrow(() -> new ModelNotFoundException("fullname", ocrRequest.getModel()));

        Map<String, Object> payload = objectMapper.convertValue(ocrRequest, new TypeReference<>() {});
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

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(HttpStatus.ACCEPTED, "Task enqueued successfully", data, request));
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
