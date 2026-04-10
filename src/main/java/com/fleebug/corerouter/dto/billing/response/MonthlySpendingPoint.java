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
@Schema(title = "Monthly Spending Point", description = "Monthly spending value in user dashboard trend")
public class MonthlySpendingPoint {

    @Schema(description = "Month label", example = "May")
    private String monthLabel;

    @Schema(description = "Month number (1-12)", example = "5")
    private Integer month;

    @Schema(description = "Year", example = "2026")
    private Integer year;

    @Schema(description = "Spending amount for the month", example = "12.50")
    private BigDecimal value;
}