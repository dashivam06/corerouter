package com.fleebug.corerouter.service.task;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;

import com.fleebug.corerouter.dto.task.request.TaskCreateRequest;
import com.fleebug.corerouter.dto.task.request.TaskStatusUpdateRequest;
import com.fleebug.corerouter.dto.task.response.AdminTaskAnalyticsResponse;
import com.fleebug.corerouter.dto.task.response.DailyTaskAnalyticsResponse;
import com.fleebug.corerouter.dto.task.response.PaginatedTaskListResponse;
import com.fleebug.corerouter.dto.task.response.TaskInsightsResponse;
import com.fleebug.corerouter.dto.task.response.TaskListItemResponse;
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
import com.fleebug.corerouter.service.billing.TaskBillingService;
import com.fleebug.corerouter.service.redis.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TelemetryClient telemetryClient;

    private static final String TASK_STREAM_KEY = "stream:tasks";

    private final TaskRepository taskRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ModelRepository modelRepository;
    private final RedisService redisService;
    private final TaskBillingService taskBillingService;
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
        telemetryClient.trackTrace("Task created and pushed to stream - taskId=" + saved.getTaskId(), SeverityLevel.Information, Map.of("taskId", saved.getTaskId()));
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

        if (request.getStatus() == TaskStatus.COMPLETED) {
            taskBillingService.applyDebitIfEligible(saved);
        }

        telemetryClient.trackTrace("Task status updated - taskId=" + saved.getTaskId() + ", status=" + saved.getStatus(), SeverityLevel.Information, Map.of("taskId", saved.getTaskId(), "status", saved.getStatus().toString()));
        return saved;
    }

    @Transactional(readOnly = true)
    public TaskInsightsResponse getTaskInsightsByUser(Integer userId) {
        long totalTasks = taskRepository.countByApiKey_User_UserId(userId);
        long completed = taskRepository.countByApiKey_User_UserIdAndStatus(userId, TaskStatus.COMPLETED);
        long failed = taskRepository.countByApiKey_User_UserIdAndStatus(userId, TaskStatus.FAILED);
        long processing = taskRepository.countByApiKey_User_UserIdAndStatus(userId, TaskStatus.PROCESSING);
        long queued = taskRepository.countByApiKey_User_UserIdAndStatus(userId, TaskStatus.QUEUED);

        return TaskInsightsResponse.builder()
                .totalTasks(totalTasks)
                .completed(completed)
                .failed(failed)
                .processing(processing)
                .queued(queued)
                .build();
    }

    @Transactional(readOnly = true)
    public TaskInsightsResponse getTaskInsightsForAdmin() {
        long totalTasks = taskRepository.count();
        long completed = taskRepository.countByStatus(TaskStatus.COMPLETED);
        long failed = taskRepository.countByStatus(TaskStatus.FAILED);
        long processing = taskRepository.countByStatus(TaskStatus.PROCESSING);
        long queued = taskRepository.countByStatus(TaskStatus.QUEUED);

        return TaskInsightsResponse.builder()
                .totalTasks(totalTasks)
                .completed(completed)
                .failed(failed)
                .processing(processing)
                .queued(queued)
                .build();
    }

    @Transactional(readOnly = true)
    public AdminTaskAnalyticsResponse getTaskAnalyticsForAdmin(LocalDateTime from, LocalDateTime to, TaskStatus statusFilter) {
        List<Object[]> raw = taskRepository.countPerDayAndStatusBetween(from, to, statusFilter);

        Map<LocalDate, DailyAccumulator> byDate = new HashMap<>();
        for (Object[] row : raw) {
            LocalDate date = toLocalDate(row[0]);
            TaskStatus status = (TaskStatus) row[1];
            long count = ((Number) row[2]).longValue();

            DailyAccumulator accumulator = byDate.computeIfAbsent(date, ignored -> new DailyAccumulator());
            accumulator.add(status, count);
        }

        List<DailyTaskAnalyticsResponse> dailyAnalytics = new ArrayList<>();
        LocalDate startDate = from.toLocalDate();
        LocalDate endDate = to.toLocalDate();

        long total = 0L;
        long queued = 0L;
        long processing = 0L;
        long completed = 0L;
        long failed = 0L;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DailyAccumulator day = byDate.getOrDefault(date, new DailyAccumulator());

            dailyAnalytics.add(DailyTaskAnalyticsResponse.builder()
                    .date(date)
                    .total(day.total)
                    .queued(day.queued)
                    .processing(day.processing)
                    .completed(day.completed)
                    .failed(day.failed)
                    .build());

            total += day.total;
            queued += day.queued;
            processing += day.processing;
            completed += day.completed;
            failed += day.failed;
        }

        return AdminTaskAnalyticsResponse.builder()
                .fromDate(startDate)
                .toDate(endDate)
                .statusFilter(statusFilter == null ? "ALL" : statusFilter.name())
                .dailyAnalytics(dailyAnalytics)
                .totalTasks(total)
                .queued(queued)
                .processing(processing)
                .completed(completed)
                .failed(failed)
                .build();
    }

    @Transactional(readOnly = true)
    public PaginatedTaskListResponse getTasksWithFiltersForAdmin(int page, int size, TaskStatus statusFilter) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Task> taskPage = (statusFilter == null)
                ? taskRepository.findAll(pageable)
                : taskRepository.findByStatus(statusFilter, pageable);

        Page<TaskListItemResponse> responsePage = taskPage.map(this::mapToTaskListItem);
        return PaginatedTaskListResponse.fromPage(responsePage);
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private static final class DailyAccumulator {
        private long total;
        private long queued;
        private long processing;
        private long completed;
        private long failed;

        private void add(TaskStatus status, long count) {
            total += count;
            switch (status) {
                case QUEUED -> queued += count;
                case PROCESSING -> processing += count;
                case COMPLETED -> completed += count;
                case FAILED -> failed += count;
                default -> {
                    // no-op for forward compatibility
                }
            }
        }
    }

    private TaskListItemResponse mapToTaskListItem(Task task) {
        return TaskListItemResponse.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus())
                .apiKeyId(task.getApiKey() != null ? task.getApiKey().getApiKeyId() : null)
                .modelId(task.getModel() != null ? task.getModel().getModelId() : null)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .completedAt(task.getCompletedAt())
                .processingTimeMs(task.getProcessingTimeMs())
                .build();
    }
}

