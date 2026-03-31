package com.fleebug.corerouter.entity.model;

import java.time.LocalDateTime;

import jakarta.persistence.Id;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fleebug.corerouter.enums.model.ModelStatus;
import com.fleebug.corerouter.enums.model.ModelType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer modelId;

    @Column(nullable = false, unique = true)
    private String fullname;

    @Column(nullable = false, unique = true)
    private String username;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModelType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModelStatus status;

    @Column(nullable = false)
    private String endpointUrl;

    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}