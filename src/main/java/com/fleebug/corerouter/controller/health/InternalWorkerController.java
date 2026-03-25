package com.fleebug.corerouter.controller.health;

import com.microsoft.applicationinsights.TelemetryClient;
import com.fleebug.corerouter.dto.health.request.WorkerHeartbeatRequest;
import com.fleebug.corerouter.entity.health.WorkerInstance;
import com.fleebug.corerouter.repository.health.WorkerInstanceRepository;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal/worker")
@RequiredArgsConstructor
@Hidden
public class InternalWorkerController {

    private final WorkerInstanceRepository workerInstanceRepository;
    private final TelemetryClient telemetryClient;

    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(@RequestBody WorkerHeartbeatRequest request) {
        LocalDateTime now = LocalDateTime.now();

        WorkerInstance worker = workerInstanceRepository.findById(request.getInstanceId())
                .orElseGet(() -> WorkerInstance.builder()
                        .instanceId(request.getInstanceId())
                        .startedAt(now)
                        .build());

        worker.setServiceName(request.getServiceName());
        worker.setStatus("UP");
        worker.setLastHeartbeat(now);
        worker.setReason(null);
        worker.setDownAt(null);

        workerInstanceRepository.save(worker);
        
        Map<String, String> properties = new HashMap<>();
        properties.put("instanceId", request.getInstanceId());
        telemetryClient.trackEvent("WORKER_HEARTBEAT", properties, null);

        return ResponseEntity.ok().build();
    }
}
