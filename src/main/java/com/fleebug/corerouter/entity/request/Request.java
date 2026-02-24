package com.fleebug.corerouter.entity.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fleebug.corerouter.entity.apikey.ApiKey;

import jakarta.persistence.Id;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_key_id", nullable = false)
    private ApiKey apiKey;

    @Column(nullable = true)
    private Integer modelId;

    @Column(nullable = false)
    private Integer inputTokenCount;

    @Column(nullable = false)
    private Integer outputTokenCount;

    @Column(nullable = false)
    private Integer totalTokensUsed;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal cost;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long totalTimeTaken; 
}
