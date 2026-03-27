package com.fleebug.corerouter.service.health;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.fleebug.corerouter.entity.health.WorkerInstance;
import com.fleebug.corerouter.repository.health.WorkerInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WorkerHealthChecker {

    private final TelemetryClient telemetryClient;
    private final WorkerInstanceRepository workerInstanceRepository;

    @Scheduled(fixedDelay = 60000)
    public void checkWorkerHeartbeats() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(60);
        List<WorkerInstance> staleWorkers = workerInstanceRepository
                .findByStatusAndLastHeartbeatBefore("UP", threshold);

        for (WorkerInstance worker : staleWorkers) {
            worker.setStatus("DOWN");
            worker.setReason("No heartbeat for 60s (likely scaled down, terminated, or disconnected)");
            worker.setDownAt(LocalDateTime.now());
            workerInstanceRepository.save(worker);

            telemetryClient.trackTrace("WORKER_DOWN reason=heartbeat_timeout", SeverityLevel.Warning, Map.of(
                    "event", "WORKER_DOWN",
                    "instanceId", worker.getInstanceId(),
                    "reason", "No heartbeat for 60s (likely scaled down, terminated, or disconnected)"
            ));
        }
    }
}
