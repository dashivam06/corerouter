package com.fleebug.corerouter.dto.billing.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardOverviewResponse {

    private BigDecimal totalEarnings;
    private BigDecimal totalEarningsChangeFromPastMonthPercent;
    private BigDecimal todayEarning;
    private Long tasksProcessedToday;
    private Long activeUsersToday;
    private List<HourlyCountPoint> taskVolume24h;
    private RevenueTrendResponse revenueTrend;
    private List<String> recentActivity;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HourlyCountPoint {
        private int hour; // 0-23 UTC
        private String labelUtc; // e.g. 14:00
        private Long value;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HourlyAmountPoint {
        private int hour; // 0-23 UTC
        private String labelUtc; // e.g. 14:00
        private BigDecimal value;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevenueTrendResponse {
        private List<HourlyAmountPoint> today;
        private List<HourlyAmountPoint> yesterday;
        private List<HourlyAmountPoint> sevenDaysAgo;
    }
}
