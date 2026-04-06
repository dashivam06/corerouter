package com.fleebug.corerouter.controller.billing;

import com.fleebug.corerouter.dto.billing.response.AdminDashboardOverviewResponse;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.entity.activity.ActivityLog;
import com.fleebug.corerouter.entity.task.Task;
import com.fleebug.corerouter.enums.task.TaskStatus;
import com.fleebug.corerouter.repository.activity.ActivityLogRepository;
import com.fleebug.corerouter.repository.task.TaskRepository;
import com.fleebug.corerouter.security.details.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Admin dashboard overview metrics")
public class AdminDashboardController {

    private static final DateTimeFormatter HOUR_LABEL = DateTimeFormatter.ofPattern("HH:00");

    private final TaskRepository taskRepository;
    private final ActivityLogRepository activityLogRepository;

    @Operation(summary = "Get dashboard overview", description = "Get fixed dashboard overview with UTC-based insights, 24h task volume and revenue trend for today/yesterday/7-days-ago")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dashboard overview retrieved successfully")
    })
    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ApiResponse<AdminDashboardOverviewResponse>> getDashboardOverview(HttpServletRequest request,
                                                     Authentication authentication) {
        LocalDateTime nowUtc = LocalDateTime.now(Clock.systemUTC());
        LocalDateTime todayStartUtc = LocalDate.now(ZoneOffset.UTC).atStartOfDay();
        LocalDateTime monthStartUtc = nowUtc.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime lastMonthStartUtc = monthStartUtc.minusMonths(1);

        BigDecimal totalEarnings = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal todayEarning = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalEarningsChangeFromPastMonthPercent = BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        Long tasksProcessedToday = 0L;
        Long activeUsersToday = 0L;
        List<AdminDashboardOverviewResponse.HourlyCountPoint> taskVolume24h = Collections.emptyList();
        AdminDashboardOverviewResponse.RevenueTrendResponse revenueTrend = AdminDashboardOverviewResponse.RevenueTrendResponse.builder()
                .today(buildEmptyRevenuePoints())
                .yesterday(buildEmptyRevenuePoints())
                .sevenDaysAgo(buildEmptyRevenuePoints())
                .build();
        List<String> recentActivity = Collections.emptyList();

        try {
            totalEarnings = taskRepository.sumTotalCostByStatus(TaskStatus.COMPLETED).setScale(2, RoundingMode.HALF_UP);
            todayEarning = taskRepository.sumTotalCostByStatusAndCompletedAtBetween(TaskStatus.COMPLETED, todayStartUtc, nowUtc)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal thisMonthRevenue = taskRepository.sumTotalCostByStatusAndCompletedAtBetween(TaskStatus.COMPLETED, monthStartUtc, nowUtc);
            BigDecimal lastMonthRevenue = taskRepository.sumTotalCostByStatusAndCompletedAtBetween(
                    TaskStatus.COMPLETED,
                    lastMonthStartUtc,
                    monthStartUtc.minusNanos(1)
            );
            totalEarningsChangeFromPastMonthPercent = calculatePercentChange(thisMonthRevenue, lastMonthRevenue);
        } catch (RuntimeException ignored) {
            // Keep defaults so dashboard still loads.
        }

        try {
            tasksProcessedToday = taskRepository.countByStatusAndCompletedAtBetween(TaskStatus.COMPLETED, todayStartUtc, nowUtc);
            activeUsersToday = taskRepository.countDistinctUsersByCreatedAtBetween(todayStartUtc, nowUtc);
            taskVolume24h = buildTaskVolume24h(nowUtc);
        } catch (RuntimeException ignored) {
            // Keep defaults so dashboard still loads.
        }

        try {
            revenueTrend = AdminDashboardOverviewResponse.RevenueTrendResponse.builder()
                    .today(buildRevenueTrendForDay(todayStartUtc))
                    .yesterday(buildRevenueTrendForDay(todayStartUtc.minusDays(1)))
                    .sevenDaysAgo(buildRevenueTrendForDay(todayStartUtc.minusDays(7)))
                    .build();
        } catch (RuntimeException ignored) {
            // Keep defaults so dashboard still loads.
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            recentActivity = activityLogRepository.findTop10ByUserOrderByCreatedAtDesc(userDetails.getUser())
                    .stream()
                    .map(this::formatActivity)
                    .toList();
        } catch (RuntimeException ignored) {
            // Keep defaults so dashboard still loads.
        }

        AdminDashboardOverviewResponse response = AdminDashboardOverviewResponse.builder()
                .totalEarnings(totalEarnings)
                .totalEarningsChangeFromPastMonthPercent(totalEarningsChangeFromPastMonthPercent)
                .todayEarning(todayEarning)
                .tasksProcessedToday(tasksProcessedToday)
                .activeUsersToday(activeUsersToday)
                .taskVolume24h(taskVolume24h)
                .revenueTrend(revenueTrend)
                .recentActivity(recentActivity)
                .build();

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Dashboard overview retrieved successfully", response, request));
    }

    private List<AdminDashboardOverviewResponse.HourlyCountPoint> buildTaskVolume24h(LocalDateTime nowUtc) {
        LocalDateTime hourEnd = nowUtc.withMinute(0).withSecond(0).withNano(0);
        LocalDateTime hourStart = hourEnd.minusHours(23);

        List<Task> tasks = taskRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(hourStart, nowUtc);

        Map<LocalDateTime, Long> buckets = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            LocalDateTime bucket = hourStart.plusHours(i);
            buckets.put(bucket, 0L);
        }

        for (Task task : tasks) {
            LocalDateTime createdAt = task.getCreatedAt();
            if (createdAt == null) {
                continue;
            }
            LocalDateTime bucket = createdAt.withMinute(0).withSecond(0).withNano(0);
            if (buckets.containsKey(bucket)) {
                buckets.put(bucket, buckets.get(bucket) + 1);
            }
        }

        List<AdminDashboardOverviewResponse.HourlyCountPoint> points = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            LocalDateTime bucket = hourStart.plusHours(i);
            points.add(AdminDashboardOverviewResponse.HourlyCountPoint.builder()
                    .hour(bucket.getHour())
                    .labelUtc(bucket.format(HOUR_LABEL))
                    .value(buckets.getOrDefault(bucket, 0L))
                    .build());
        }

        return points;
    }

    private List<AdminDashboardOverviewResponse.HourlyAmountPoint> buildRevenueTrendForDay(LocalDateTime dayStartUtc) {
        LocalDateTime dayEndUtc = dayStartUtc.plusDays(1).minusNanos(1);

        List<Object[]> rows;
        try {
            rows = taskRepository.sumTotalCostByHourBetween(dayStartUtc, dayEndUtc, TaskStatus.COMPLETED);
        } catch (RuntimeException ex) {
            rows = buildRevenueRowsInMemory(dayStartUtc, dayEndUtc);
        }

        Map<Integer, BigDecimal> hourlyMap = new HashMap<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            Integer hour = ((Number) row[0]).intValue();
            BigDecimal amount = toBigDecimal(row[1]);
            hourlyMap.put(hour, amount);
        }

        List<AdminDashboardOverviewResponse.HourlyAmountPoint> points = new ArrayList<>();

        for (int hour = 0; hour < 24; hour++) {
            BigDecimal value = hourlyMap.getOrDefault(hour, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            points.add(AdminDashboardOverviewResponse.HourlyAmountPoint.builder()
                    .hour(hour)
                    .labelUtc(String.format("%02d:00", hour))
                    .value(value)
                    .build());
        }

        return points;
    }

    private List<Object[]> buildRevenueRowsInMemory(LocalDateTime dayStartUtc, LocalDateTime dayEndUtc) {
        Map<Integer, BigDecimal> sums = new HashMap<>();
        for (Task task : taskRepository.findAll()) {
            if (task.getStatus() != TaskStatus.COMPLETED || task.getCompletedAt() == null) {
                continue;
            }
            LocalDateTime completedAt = task.getCompletedAt();
            if (completedAt.isBefore(dayStartUtc) || completedAt.isAfter(dayEndUtc)) {
                continue;
            }
            int hour = completedAt.getHour();
            BigDecimal cost = task.getTotalCost() == null ? BigDecimal.ZERO : task.getTotalCost();
            sums.put(hour, sums.getOrDefault(hour, BigDecimal.ZERO).add(cost));
        }

        List<Object[]> rows = new ArrayList<>();
        for (Map.Entry<Integer, BigDecimal> entry : sums.entrySet()) {
            rows.add(new Object[]{entry.getKey(), entry.getValue()});
        }
        return rows;
    }

    private List<AdminDashboardOverviewResponse.HourlyAmountPoint> buildEmptyRevenuePoints() {
        List<AdminDashboardOverviewResponse.HourlyAmountPoint> points = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            points.add(AdminDashboardOverviewResponse.HourlyAmountPoint.builder()
                    .hour(hour)
                    .labelUtc(String.format("%02d:00", hour))
                    .value(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        return points;
    }

    private BigDecimal toBigDecimal(Object raw) {
        if (raw == null) {
            return BigDecimal.ZERO;
        }
        if (raw instanceof BigDecimal value) {
            return value;
        }
        if (raw instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculatePercentChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }

    private String formatActivity(ActivityLog log) {
        String action = log.getAction() == null || log.getAction().isBlank() ? "Activity" : log.getAction();
        String details = log.getDetails() == null || log.getDetails().isBlank() ? "no details" : log.getDetails();
        return action + " · " + details;
    }
}
