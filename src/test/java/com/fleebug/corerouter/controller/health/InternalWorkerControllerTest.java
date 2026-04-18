package com.fleebug.corerouter.controller.health;

import com.fleebug.corerouter.dto.health.request.WorkerHeartbeatRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalWorkerControllerTest {

    @Mock
    private WorkerInstanceRepository workerInstanceRepository;

    @Mock
    private TelemetryClient telemetryClient;

    @InjectMocks
    private InternalWorkerController internalWorkerController;

    @Captor
    private ArgumentCaptor<WorkerInstance> workerCaptor;

    @Test
    // UT-TASK-06: Worker heartbeat -> POST /internal/heartbeat returning 200, saves worker with status UP
    void heartbeat_CreatesOrUpdatesWorkerStatusToUp() {
        WorkerHeartbeatRequest request = new WorkerHeartbeatRequest();
        request.setInstanceId("worker-node-1");
        request.setServiceName("llm-worker");
        request.setStatus("UP");

        when(workerInstanceRepository.findById("worker-node-1")).thenReturn(Optional.empty());

        ResponseEntity<Void> response = internalWorkerController.heartbeat(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(workerInstanceRepository).save(workerCaptor.capture());
        WorkerInstance savedWorker = workerCaptor.getValue();
        
        assertEquals("worker-node-1", savedWorker.getInstanceId());
        assertEquals("llm-worker", savedWorker.getServiceName());
        assertEquals("UP", savedWorker.getStatus());
        assertNotNull(savedWorker.getLastHeartbeat());
        assertNull(savedWorker.getDownAt());
        assertNull(savedWorker.getReason());
    }

    @Test
    // Ensures a worker intentionally sending DOWN records its shutdown status precisely
    void heartbeat_MarksWorkerDownIfStatusIsDown() {
        WorkerHeartbeatRequest request = new WorkerHeartbeatRequest();
        request.setInstanceId("worker-node-2");
        request.setServiceName("llm-worker");
        request.setStatus("DOWN");

        when(workerInstanceRepository.findById("worker-node-2")).thenReturn(Optional.empty());

        internalWorkerController.heartbeat(request);

        verify(workerInstanceRepository).save(workerCaptor.capture());
        WorkerInstance savedWorker = workerCaptor.getValue();
        
        assertEquals("DOWN", savedWorker.getStatus());
        assertEquals("Scaled down or shutting down (worker reported DOWN)", savedWorker.getReason());
        assertNotNull(savedWorker.getDownAt());
    }
}
