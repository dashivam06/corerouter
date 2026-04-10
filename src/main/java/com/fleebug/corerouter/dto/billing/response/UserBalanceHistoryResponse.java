package com.fleebug.corerouter.dto.billing.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "User Balance History Response", description = "Top-up based balance history for requested period")
public class UserBalanceHistoryResponse {

    @Schema(description = "Applied period label", example = "30days")
    private String period;

    @Schema(description = "Applied start date-time")
    private LocalDateTime fromDate;

    @Schema(description = "Applied end date-time")
    private LocalDateTime toDate;

    @Schema(description = "Total top-up amount in range", example = "1000.00")
    private BigDecimal totalTopUp;

    @Schema(description = "Daily top-up trend")
    private List<DailySpendingPoint> balanceHistory;
}