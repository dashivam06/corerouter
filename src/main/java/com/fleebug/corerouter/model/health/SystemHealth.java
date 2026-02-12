package com.fleebug.corerouter.model.health;

import java.time.LocalDateTime;

import com.fleebug.corerouter.enums.health.HealthStatus;
import com.fleebug.corerouter.model.model.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "system_health")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemHealth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer healthId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private Model model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HealthStatus status;

    @Column(nullable = false)
    private Integer queueLength;

    @Column(nullable = false)
    private Integer concurrencyLevel;

    @Column(length = 500)
    private String description;

    @Column
    private Double cpuUsage; // percentage, optional

    @Column
    private Double memoryUsage; // percentage, optional

    @Column
    private Long lastResponseTime; // in milliseconds, optional

    @Column(nullable = false)
    private LocalDateTime checkedAt;
}
