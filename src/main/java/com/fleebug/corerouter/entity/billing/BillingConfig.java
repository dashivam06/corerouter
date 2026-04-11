package com.fleebug.corerouter.entity.billing;

import java.time.LocalDateTime;
import java.math.BigDecimal;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fleebug.corerouter.entity.model.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "billing_configs",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_billing_config_model",
        columnNames = {"model_id"}
    )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer billingId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private Model model;

    @Column(nullable = false, length = 30)
    private String pricingType;

    // JSON rates: PER_TOKEN → {"inputRate":0.00003,"outputRate":0.00006}, PER_IMAGE → {"rate":0.01}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String pricingMetadata;

    @Column(name = "charge_multiplier", nullable = false, precision = 8, scale = 4, columnDefinition = "numeric(8,4) default 1.0000")
    @Builder.Default
    private BigDecimal chargeMultiplier = new BigDecimal("1.0000");

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}