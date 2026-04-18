package com.fleebug.corerouter.service.health;

import com.fleebug.corerouter.entity.health.WorkerInstance;
import com.fleebug.corerouter.repository.health.WorkerInstanceRepository;
import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerHealthCheckerTest {

    @Mock
    private TelemetryClient telemetryClient;

    @Mock
    private WorkerInstanceRepository workerInstanceRepository;

    @InjectMocks
    private WorkerHealthChecker workerHealthChecker;

    @Captor
    private ArgumentCaptor<WorkerInstance> workerCaptor;

    @Test
    // UT-TASK-07: Stale detection -> DB showing worker status=DOWN after threshold.
    void checkWorkerHeartbeats_MarksStaleWorkersAsDown() {
        LocalDateTime staleTime = LocalDateTime.now().minusSeconds(120);

        WorkerInstance staleWorker = WorkerInstance.builder()
                .instanceId("worker-stale-1")
                .status("UP")
                .lastHeartbeat(staleTime)
                .build();

        // We mock the repository answering queries for stale records
        when(workerInstanceRepository.findByStatusAndLastHeartbeatBefore(
                eq("UP"), any(LocalDateTime.class)))
                .thenReturn(List.of(staleWorker));

        // Act
        workerHealthChecker.checkWorkerHeartbeats();

        // Assert
        verify(workerInstanceRepository).save(workerCaptor.capture());
        WorkerInstance updatedWorker = workerCaptor.getValue();
        
        assertEquals("worker-stale-1", updatedWorker.getInstanceId());
        assertEquals("DOWN", updatedWorker.getStatus(), "Stale worker MUST be marked DOWN");
        assertEquals("No heartbeat for 60s (likely scaled down, terminated, or disconnected)", updatedWorker.getReason());
        assertNotNull(updatedWorker.getDownAt());
    }

    @Test
    // Verifies that if no workers are stale, the DB is not hit with updates
    void checkWorkerHeartbeats_DoesNothingWhenNoStaleWorkers() {
        when(workerInstanceRepository.findByStatusAndLastHeartbeatBefore(
                eq("UP"), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // Act
        workerHealthChecker.checkWorkerHeartbeats();

        // Assert
        verify(workerInstanceRepository, never()).save(any(WorkerInstance.class));
    }
}
