package com.fleebug.corerouter.dto.billing.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents daily earned money (top-ups) for all users.
 * Shows aggregated earnings per day.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyEarningsResponse {

    private LocalDate date;
    private BigDecimal totalEarned;
    private Integer transactionCount;
}
