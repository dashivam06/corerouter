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
@Schema(title = "User Balance History Response", description = "Balance history with credit and debit timeline for requested period")
public class UserBalanceHistoryResponse {

    @Schema(description = "Applied period label", example = "30days")
    private String period;

    @Schema(description = "Applied start date-time")
    private LocalDateTime fromDate;

    @Schema(description = "Applied end date-time")
    private LocalDateTime toDate;

    @Schema(description = "Total top-up amount in range", example = "1000.00")
    private BigDecimal totalTopUp;

    @Schema(description = "Total debit amount in range from completed task costs", example = "250.50")
    private BigDecimal totalDebit;

    @Schema(description = "Net change in range (credit - debit)", example = "749.50")
    private BigDecimal netChange;

    @Schema(description = "Balance at start of range before applying credits and debits", example = "1000.00")
    private BigDecimal openingBalance;

    @Schema(description = "Balance at end of range after applying all credits and debits", example = "10900.00")
    private BigDecimal closingBalance;

    @Schema(description = "Daily remaining balance trend")
    private List<DailySpendingPoint> balanceHistory;

    @Schema(description = "Daily credit/debit/net breakdown")
    private List<BalanceHistoryDay> dailyBreakdown;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceHistoryDay {
        @Schema(description = "Date label", example = "2026-04-11")
        private String date;

        @Schema(description = "Total credited on date", example = "100.00")
        private BigDecimal credit;

        @Schema(description = "Total debited on date", example = "20.00")
        private BigDecimal debit;

        @Schema(description = "Net for date (credit - debit)", example = "80.00")
        private BigDecimal net;

        @Schema(description = "Remaining balance at end of date", example = "10900.00")
        private BigDecimal remainingBalance;

        @Schema(description = "Latest task-level remaining balance snapshot recorded on date (if any)", example = "10890.00")
        private BigDecimal taskRemainingBalance;
    }

}