package com.fleebug.corerouter.model.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Id;

import com.fleebug.corerouter.enums.model.ModelStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "models")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer modelId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column
    private Long parameterCount;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal pricePer1kTokens;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModelStatus status;

    @Column(nullable = false, length = 500)
    private String endpointUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String type; 
}
