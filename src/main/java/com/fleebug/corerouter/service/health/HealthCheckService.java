package com.fleebug.corerouter.service.health;

import com.microsoft.applicationinsights.TelemetryClient;
import com.fleebug.corerouter.entity.health.WorkerInstance;
import com.fleebug.corerouter.repository.health.WorkerInstanceRepository;
import com.fleebug.corerouter.repository.user.UserRepository;
import com.fleebug.corerouter.util.HttpClientUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final TelemetryClient telemetryClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final WorkerInstanceRepository workerInstanceRepository;
    private final HttpClientUtil httpClientUtil;

    @Value("${vllm.host}")
    private String vllmHost;

    public Map<String, Object> checkRedis() {
        long start = System.currentTimeMillis();
        try {
            redisTemplate.opsForValue().get("health-check");
            long latency = System.currentTimeMillis() - start;

            Map<String, Object> result = new HashMap<>();
            result.put("status", "UP");
            result.put("latency", latency + "ms");
            return result;
        } catch (Exception e) {
            telemetryClient.trackException(e, Map.of("event", "REDIS_DOWN", "reason", String.valueOf(e.getMessage())), null);
            return down(rootCauseMessage(e));
        }
    }

    public Map<String, Object> checkVllm() {
        long start = System.currentTimeMillis();
        try {
            httpClientUtil.get(vllmHost + "/health", Map.of(), 5000, 5000);

            long latency = System.currentTimeMillis() - start;
            Map<String, Object> result = new HashMap<>();
            result.put("status", "UP");
            result.put("latency", latency + "ms");
            return result;
        } catch (Exception e) {
            telemetryClient.trackException(e, Map.of("event", "VLLM_DOWN", "reason", String.valueOf(e.getMessage())), null);
            return down(rootCauseMessage(e));
        }
    }

    public Map<String, Object> checkDatabase() {
        long start = System.currentTimeMillis();
        try {
            userRepository.count();
            long latency = System.currentTimeMillis() - start;

            Map<String, Object> result = new HashMap<>();
            result.put("status", "UP");
            result.put("latency", latency + "ms");
            return result;
        } catch (Exception e) {
            telemetryClient.trackException(e, Map.of("event", "DB_DOWN", "reason", String.valueOf(e.getMessage())), null);
            return down(rootCauseMessage(e));
        }
    }

    public Map<String, Object> checkWorkers() {
        // Use UTC to align with DB timestamps
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime threshold = now.minusSeconds(60);
        LocalDateTime recentDownThreshold = now.minusMinutes(10);

        List<WorkerInstance> allWorkers = workerInstanceRepository.findAllByOrderByStartedAtDesc();

        // Active workers must be UP and have a fresh heartbeat
        List<WorkerInstance> activeWorkers = allWorkers
                .stream()
                .filter(w -> "UP".equalsIgnoreCase(w.getStatus()))
                .filter(w -> w.getLastHeartbeat() != null && w.getLastHeartbeat().isAfter(threshold))
                .toList();

        List<WorkerInstance> recentDownWorkers = allWorkers
                .stream()
                .filter(w -> "DOWN".equalsIgnoreCase(w.getStatus()))
                .filter(w -> w.getDownAt() != null && w.getDownAt().isAfter(recentDownThreshold))
                .toList();

        Map<String, Object> result = new HashMap<>();
        if (activeWorkers.isEmpty()) {
            result.put("status", "DOWN");
            result.put("running", 0);

            if (!recentDownWorkers.isEmpty()) {
                result.put("reason", "Workers appear scaled down or terminated recently; no active heartbeat in last 60s");
                result.put("recentDownCount", recentDownWorkers.size());
                result.put("recentDownInstances", recentDownWorkers.stream()
                        .map(worker -> {
                            Map<String, Object> data = new HashMap<>();
                            data.put("instanceId", worker.getInstanceId());
                            data.put("serviceName", worker.getServiceName());
                            data.put("downAt", worker.getDownAt());
                            data.put("reason", worker.getReason());
                            return data;
                        })
                        .toList());
            } else {
                result.put("reason", "No active workers reported heartbeat in the last 60s");
            }

            return result;
        }

        // Group workers by service name (e.g., "otp-worker", "ocr-worker")
        Map<String, List<Map<String, Object>>> workersByGroup = activeWorkers.stream()
                .collect(Collectors.groupingBy(WorkerInstance::getServiceName,
                        Collectors.mapping(worker -> {
                            Map<String, Object> workerData = new HashMap<>();
                            workerData.put("instanceId", worker.getInstanceId());
                            workerData.put("startedAt", worker.getStartedAt());
                            workerData.put("lastHeartbeat", worker.getLastHeartbeat());
                            workerData.put("status", worker.getStatus());
                            return workerData;
                        }, Collectors.toList())));
        result.put("status", "UP");
        result.put("totalRunning", activeWorkers.size());
        if (!recentDownWorkers.isEmpty()) {
            result.put("notice", "Some workers were recently scaled down or restarted");
            result.put("recentDownCount", recentDownWorkers.size());
        }
        result.put("groups", workersByGroup);
        return result;
    }

    private Map<String, Object> down(String reason) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "DOWN");
        result.put("reason", reason);
        return result;
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? "Unknown error" : message;
    }

}
