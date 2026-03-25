package com.fleebug.corerouter.service.health;

import com.fleebug.corerouter.entity.health.WorkerInstance;
import com.fleebug.corerouter.repository.health.WorkerInstanceRepository;
import com.fleebug.corerouter.repository.user.UserRepository;
import com.fleebug.corerouter.util.HttpClientUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckService {

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
            log.error("event=REDIS_DOWN reason={}", e.getMessage());
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
            log.error("event=VLLM_DOWN reason={}", e.getMessage());
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
            log.error("event=DB_DOWN reason={}", e.getMessage());
            return down(rootCauseMessage(e));
        }
    }

    public Map<String, Object> checkWorkers() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(60);
        List<WorkerInstance> activeWorkers = workerInstanceRepository
                .findByStatusAndLastHeartbeatAfter("UP", threshold);

        Map<String, Object> result = new HashMap<>();
        if (activeWorkers.isEmpty()) {
            result.put("status", "DOWN");
            result.put("running", 0);
            result.put("reason", "No heartbeat received");
            return result;
        }

        List<Map<String, Object>> instances = activeWorkers.stream().map(worker -> {
            Map<String, Object> workerData = new HashMap<>();
            workerData.put("instanceId", worker.getInstanceId());
            workerData.put("lastHeartbeat", worker.getLastHeartbeat());
            workerData.put("startedAt", worker.getStartedAt());
            return workerData;
        }).toList();

        result.put("status", "UP");
        result.put("running", activeWorkers.size());
        result.put("instances", instances);
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
