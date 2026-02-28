package com.fleebug.corerouter.entity.billing;

import com.fleebug.corerouter.entity.apikey.ApiKey;
import com.fleebug.corerouter.entity.model.Model;
import com.fleebug.corerouter.entity.task.Task;
import com.fleebug.corerouter.enums.billing.UsageUnitType;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// One Task → multiple UsageRecords (e.g. LLM: INPUT_TOKENS + OUTPUT_TOKENS).
// apiKey & model are denormalized from Task for billing aggregation perf.
@Entity
@Table(
    name = "usage_records",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_usage_task_unit",
        columnNames = {"task_id", "usage_unit_type"}
    ),
    indexes = {
        @Index(name = "idx_usage_task_id", columnList = "task_id"),
        @Index(name = "idx_usage_api_key_id", columnList = "api_key_id"),
        @Index(name = "idx_usage_model_id", columnList = "model_id"),
        @Index(name = "idx_usage_recorded_at", columnList = "recorded_at"),
        @Index(name = "idx_usage_unit_type", columnList = "usage_unit_type"),
        @Index(name = "idx_usage_billing_lookup", columnList = "api_key_id, recorded_at, usage_unit_type")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long usageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_key_id", nullable = false)
    private ApiKey apiKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private Model model;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_unit_type", nullable = false, length = 30)
    private UsageUnitType usageUnitType;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    // Snapshot from BillingConfig at record time — immune to price changes
    @Column(nullable = false, precision = 18, scale = 10)
    private BigDecimal ratePerUnit;

    // quantity * ratePerUnit
    @Column(nullable = false, precision = 18, scale = 10)
    private BigDecimal cost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_config_id")
    private BillingConfig billingConfig;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
}
