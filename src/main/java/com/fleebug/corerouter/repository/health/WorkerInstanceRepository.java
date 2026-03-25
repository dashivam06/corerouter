package com.fleebug.corerouter.repository.health;

import com.fleebug.corerouter.entity.health.WorkerInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkerInstanceRepository extends JpaRepository<WorkerInstance, String> {

    List<WorkerInstance> findByStatus(String status);

    List<WorkerInstance> findAllByOrderByStartedAtDesc();

    List<WorkerInstance> findByStatusAndLastHeartbeatAfter(String status, LocalDateTime threshold);

    List<WorkerInstance> findByStatusAndLastHeartbeatBefore(String status, LocalDateTime threshold);

    long countByStatus(String status);
}
