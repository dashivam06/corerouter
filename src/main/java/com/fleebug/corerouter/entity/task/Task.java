package com.fleebug.corerouter.entity.task;

import com.fleebug.corerouter.entity.apikey.ApiKey;
import com.fleebug.corerouter.entity.model.Model;
import com.fleebug.corerouter.enums.task.TaskStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity
@Table(name = "tasks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    private String taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_key_id", nullable = false)
    private ApiKey apiKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private Model model;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String requestPayload;

    @Column(columnDefinition = "TEXT")
    private String resultPayload;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    // Denormalized SUM of UsageRecord.cost — updated by UsageService
    @Column(precision = 12, scale = 6)
    private BigDecimal totalCost;

    private Long processingTimeMs;

    // Raw provider response for audit/debugging — billing uses UsageRecord
    @Column(columnDefinition = "jsonb")
    private String usageMetadata;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}