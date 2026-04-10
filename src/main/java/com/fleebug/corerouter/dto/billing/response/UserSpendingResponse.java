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
@Schema(title = "User Spending Response", description = "Spending summary and daily trend for a requested range")
public class UserSpendingResponse {

    @Schema(description = "Applied period label", example = "month")
    private String filterPeriod;

    @Schema(description = "Applied from datetime")
    private LocalDateTime fromDate;

    @Schema(description = "Applied to datetime")
    private LocalDateTime toDate;

    @Schema(description = "Total spending in range", example = "120.25")
    private BigDecimal totalSpending;

    @Schema(description = "Daily spending trend in range")
    private List<DailySpendingPoint> dailyTrend;
}