package com.fleebug.corerouter.dto.billing.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "Daily Spending Point", description = "Daily spend value for spending trend")
public class DailySpendingPoint {

    @Schema(description = "Date label in ISO format", example = "2026-04-11")
    private String date;

    @Schema(description = "Spending amount for the date", example = "12.50")
    private BigDecimal value;
}