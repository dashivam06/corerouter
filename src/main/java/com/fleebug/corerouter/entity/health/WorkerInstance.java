package com.fleebug.corerouter.entity.health;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "worker_instances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerInstance {

    @Id
    private String instanceId;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private String status;

    @Column
    private String reason;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime lastHeartbeat;

    @Column
    private LocalDateTime downAt;
}
