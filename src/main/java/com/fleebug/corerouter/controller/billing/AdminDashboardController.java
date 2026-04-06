package com.fleebug.corerouter.controller.billing;

import com.fleebug.corerouter.dto.billing.response.AdminDashboardOverviewResponse;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.entity.task.Task;
import com.fleebug.corerouter.enums.task.TaskStatus;
import com.fleebug.corerouter.repository.task.TaskRepository;
import com.fleebug.corerouter.service.payment.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Admin dashboard overview metrics")
public class AdminDashboardController {

    private static final DateTimeFormatter HOUR_LABEL = DateTimeFormatter.ofPattern("HH:00");

    private final TransactionService transactionService;
    private final TaskRepository taskRepository;

    @Operation(summary = "Get dashboard overview", description = "Get fixed dashboard overview with UTC-based insights, 24h task volume and revenue trend for today/yesterday/7-days-ago")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dashboard overview retrieved successfully")
    })
    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminDashboardOverviewResponse>> getDashboardOverview(HttpServletRequest request) {
        LocalDateTime nowUtc = LocalDateTime.now(Clock.systemUTC());
        LocalDateTime todayStartUtc = LocalDate.now(ZoneOffset.UTC).atStartOfDay();
        LocalDateTime monthStartUtc = nowUtc.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime lastMonthStartUtc = monthStartUtc.minusMonths(1);

        BigDecimal totalEarnings = transactionService.getTopUpAmountAllTime().setScale(2, RoundingMode.HALF_UP);
        BigDecimal todayEarning = transactionService.getTopUpAmountByPeriod(todayStartUtc, nowUtc).setScale(2, RoundingMode.HALF_UP);
        BigDecimal thisMonthRevenue = transactionService.getTopUpAmountByPeriod(monthStartUtc, nowUtc);
        BigDecimal lastMonthRevenue = transactionService.getTopUpAmountByPeriod(lastMonthStartUtc, monthStartUtc.minusNanos(1));

        BigDecimal totalEarningsChangeFromPastMonthPercent = calculatePercentChange(thisMonthRevenue, lastMonthRevenue);

        Long tasksProcessedToday = taskRepository.countByStatusAndCompletedAtBetween(TaskStatus.COMPLETED, todayStartUtc, nowUtc);
        Long activeUsersToday = taskRepository.countDistinctUsersByCreatedAtBetween(todayStartUtc, nowUtc);

        List<AdminDashboardOverviewResponse.HourlyCountPoint> taskVolume24h = buildTaskVolume24h(nowUtc);

        AdminDashboardOverviewResponse.RevenueTrendResponse revenueTrend = AdminDashboardOverviewResponse.RevenueTrendResponse.builder()
                .today(buildRevenueTrendForDay(todayStartUtc))
                .yesterday(buildRevenueTrendForDay(todayStartUtc.minusDays(1)))
                .sevenDaysAgo(buildRevenueTrendForDay(todayStartUtc.minusDays(7)))
                .build();

        List<String> recentActivity = taskRepository.findTop10ByStatusAndCompletedAtIsNotNullOrderByCompletedAtDesc(TaskStatus.COMPLETED)
                .stream()
                .map(task -> "Task completed · " + task.getTaskId())
                .toList();

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

        Map<Integer, BigDecimal> hourlyMap = transactionService.getTopUpAmountByHour(dayStartUtc, dayEndUtc);
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

    private BigDecimal calculatePercentChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }
}
