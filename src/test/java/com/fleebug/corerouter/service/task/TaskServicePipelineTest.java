package com.fleebug.corerouter.service.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleebug.corerouter.dto.task.request.TaskCreateRequest;
import com.fleebug.corerouter.dto.task.request.TaskStatusUpdateRequest;
import com.fleebug.corerouter.entity.apikey.ApiKey;
import com.fleebug.corerouter.entity.model.Model;
import com.fleebug.corerouter.entity.task.Task;
import com.fleebug.corerouter.enums.apikey.ApiKeyStatus;
import com.fleebug.corerouter.enums.task.TaskStatus;
import com.fleebug.corerouter.repository.apikey.ApiKeyRepository;
import com.fleebug.corerouter.repository.model.ModelRepository;
import com.fleebug.corerouter.repository.task.TaskRepository;
import com.fleebug.corerouter.service.billing.TaskBillingService;
import com.fleebug.corerouter.service.redis.RedisService;
import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServicePipelineTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TaskBillingService taskBillingService;

    @Mock
    private TelemetryClient telemetryClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TaskService taskService;

    @Captor
    private ArgumentCaptor<Task> taskCaptor;

    @Captor
    private ArgumentCaptor<Map<String, String>> redisPayloadCaptor;

    private ApiKey activeApiKey;
    private Model testModel;

    @BeforeEach
    void setUp() {
        activeApiKey = ApiKey.builder().apiKeyId(1).status(ApiKeyStatus.ACTIVE).build();
        testModel = Model.builder().modelId(10).fullname("gpt-4-test").build();
    }

    @Test
    // UT-TASK-01: Task creation -> Check it saves with QUEUED status and generates a taskId.
    // UT-TASK-02: Task pushed to Redis stream -> Check redisService.appendToStream was called with correct payload.
    void createTask_SavesTaskAsQueued_AndPushesToRedisStream() throws JsonProcessingException {
        TaskCreateRequest request = TaskCreateRequest.builder()
                .apiKeyId(1)
                .modelId(10)
                .payload(Map.of("prompt", "Hello world"))
                .build();

        when(apiKeyRepository.findById(1)).thenReturn(Optional.of(activeApiKey));
        when(modelRepository.findById(10)).thenReturn(Optional.of(testModel));
        
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            return t; // Returns the exact task constructed internally with random UUID
        });

        Task result = taskService.createTask(request);

        // Verification for TASK-01
        assertNotNull(result.getTaskId(), "Task must have an auto-generated UUID");
        assertEquals(TaskStatus.QUEUED, result.getStatus(), "Initial state of a new task must be QUEUED");
        assertEquals("{\"prompt\":\"Hello world\"}", result.getRequestPayload(), "Payload must be strictly serialized");

        verify(taskRepository).save(taskCaptor.capture());
        Task capturedTask = taskCaptor.getValue();
        assertEquals(TaskStatus.QUEUED, capturedTask.getStatus());

        // Verification for TASK-02
        verify(redisService).appendToStream(eq("stream:tasks"), redisPayloadCaptor.capture());
        Map<String, String> redisData = redisPayloadCaptor.getValue();
        
        assertEquals(result.getTaskId(), redisData.get("taskId"), "Redis stream must contain the identical task ID");
        assertEquals("1", redisData.get("apiKeyId"));
        assertEquals("10", redisData.get("modelId"));
    }

    @Test
    // UT-TASK-03,04,05: Status transitions -> Check updateTaskStatus handles state changes correctly
    void updateTaskStatus_TransitionsFromQueuedToProcessingToCompleted() {
        String taskId = "sample-task-123";
        Task existingTask = Task.builder()
                .taskId(taskId)
                .status(TaskStatus.QUEUED)
                .createdAt(java.time.LocalDateTime.now())
                .build();
                
        // Task-03: QUEUED -> PROCESSING
        TaskStatusUpdateRequest processingRequest = new TaskStatusUpdateRequest(taskId, 
                TaskStatus.PROCESSING, null, null);
        
        when(taskRepository.findByTaskId(taskId)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        Task processingTask = taskService.updateTaskStatus(processingRequest);
        assertEquals(TaskStatus.PROCESSING, processingTask.getStatus());
        
        // Task-04 & 05: PROCESSING -> COMPLETED
        existingTask.setStatus(TaskStatus.PROCESSING);
        TaskStatusUpdateRequest completedRequest = new TaskStatusUpdateRequest(taskId, 
                TaskStatus.COMPLETED, Map.of("result", "ok"), null);
        
        Task completedTask = taskService.updateTaskStatus(completedRequest);
        assertEquals(TaskStatus.COMPLETED, completedTask.getStatus());
        assertEquals("{\"result\":\"ok\"}", completedTask.getResultPayload());
        
        // Ensure billing gets triggered on COMPLETED
        verify(taskBillingService).applyDebitIfEligible(completedTask);
    }
}
