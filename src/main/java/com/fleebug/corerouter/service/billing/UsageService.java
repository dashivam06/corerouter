package com.fleebug.corerouter.service.billing;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleebug.corerouter.dto.billing.request.RecordUsageRequest;
import com.fleebug.corerouter.dto.billing.response.DailySpendingPoint;
import com.fleebug.corerouter.dto.billing.response.MonthlySpendingPoint;
import com.fleebug.corerouter.dto.billing.response.UsageRecordResponse;
import com.fleebug.corerouter.dto.billing.response.UsageSummaryItem;
import com.fleebug.corerouter.dto.billing.response.UsageSummaryResponse;
import com.fleebug.corerouter.dto.billing.response.UserBillingInsightsResponse;
import com.fleebug.corerouter.dto.billing.response.UserDashboardOverviewResponse;
import com.fleebug.corerouter.dto.billing.response.UserDashboardInsightsResponse;
import com.fleebug.corerouter.dto.billing.response.UserSpendingResponse;
import com.fleebug.corerouter.dto.billing.response.UserUsageHistoryResponse;
import com.fleebug.corerouter.dto.billing.response.UserUsageByModelTypeResponse;
import com.fleebug.corerouter.dto.billing.response.UserUsageInsightsResponse;
import com.fleebug.corerouter.entity.billing.BillingConfig;
import com.fleebug.corerouter.entity.billing.UsageRecord;
import com.fleebug.corerouter.entity.model.Model;
import com.fleebug.corerouter.entity.task.Task;
import com.fleebug.corerouter.enums.apikey.ApiKeyStatus;
import com.fleebug.corerouter.enums.billing.UsageUnitType;
import com.fleebug.corerouter.enums.model.ModelType;
import com.fleebug.corerouter.enums.task.TaskStatus;
import com.fleebug.corerouter.exception.billing.BillingCalculationException;
import com.fleebug.corerouter.exception.task.TaskNotFoundException;
import com.fleebug.corerouter.repository.apikey.ApiKeyRepository;
import com.fleebug.corerouter.repository.billing.UsageRecordRepository;
import com.fleebug.corerouter.repository.model.ModelRepository;
import com.fleebug.corerouter.repository.task.TaskRepository;
import com.fleebug.corerouter.service.otp.OtpService;
import com.fleebug.corerouter.service.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class UsageService {

    private final TelemetryClient telemetryClient;

    private final UsageRecordRepository usageRecordRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final TaskRepository taskRepository;
    private final ModelRepository modelRepository;
    private final BillingConfigService billingConfigService;
    private final TaskBillingService taskBillingService;
    private final ObjectMapper objectMapper;
    private final OtpService otpService;
    private final RedisService redisService;

    private static final String API_KEY_MONTHLY_ALERT_PREFIX = "billing:apikey:monthly-alert:";

    /**
     * Record usage for a task, compute cost from billing config, and update task total cost.
     *
     * @param request usage recording request
     * @return recorded usage response
     */
    public UsageRecordResponse recordUsage(RecordUsageRequest request) {
        telemetryClient.trackTrace("Recording usage for taskId=" + request.getTaskId() + ", unitType=" + request.getUsageUnitType() + ", quantity=" + request.getQuantity(), SeverityLevel.Information, Map.of("taskId", request.getTaskId(), "unitType", String.valueOf(request.getUsageUnitType()), "quantity", String.valueOf(request.getQuantity())));

        Task task = taskRepository.findByTaskId(request.getTaskId())
                .orElseThrow(() -> new TaskNotFoundException(request.getTaskId()));

        Model model = task.getModel();
        BillingConfig billingConfig = billingConfigService.getBillingConfigEntityByModelId(model.getModelId());

        BigDecimal ratePerUnit = extractRate(billingConfig, request.getUsageUnitType());
        BigDecimal cost = request.getQuantity().multiply(ratePerUnit).setScale(10, RoundingMode.HALF_UP);

        UsageRecord record = UsageRecord.builder()
                .task(task)
                .apiKey(task.getApiKey())
                .model(model)
                .usageUnitType(request.getUsageUnitType())
                .quantity(request.getQuantity())
                .ratePerUnit(ratePerUnit)
                .cost(cost)
                .billingConfig(billingConfig)
                .recordedAt(LocalDateTime.now())
                .build();

        UsageRecord saved = usageRecordRepository.save(record);
        telemetryClient.trackTrace("Usage recorded: usageId=" + saved.getUsageId() + ", taskId=" + request.getTaskId() + ", cost=" + cost, SeverityLevel.Information, Map.of("usageId", String.valueOf(saved.getUsageId()), "taskId", request.getTaskId(), "cost", cost.toPlainString()));

        // Update task totalCost
        updateTaskCost(task);

        // Charge wallet only after task is completed; repeated calls are idempotent.
        taskBillingService.applyDebitIfEligible(task);

        checkAndNotifyMonthlyApiKeyUsage(saved.getApiKey());

        return mapToResponse(saved);
    }

    /**
     * Get all usage records for a specific task.
     *
     * @param taskId task ID
     * @return list of usage record responses
     */
    @Transactional(readOnly = true)
    public List<UsageRecordResponse> getUsageByTaskId(String taskId) {
        return usageRecordRepository.findByTaskTaskId(taskId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get total cost for an API key within a date range.
     *
     * @param apiKeyId API key ID
     * @param from     period start
     * @param to       period end
     * @return total cost
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalCostByApiKey(Integer apiKeyId, LocalDateTime from, LocalDateTime to) {
        return usageRecordRepository.sumCostByApiKeyAndPeriod(apiKeyId, from, to);
    }

    /**
     * Get usage summary for an API key grouped by unit type.
     *
     * @param apiKeyId API key ID
     * @param from     period start
     * @param to       period end
     * @return usage summary with breakdown
     */
    @Transactional(readOnly = true)
    public UsageSummaryResponse getUsageSummaryByApiKey(Integer apiKeyId, LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = usageRecordRepository.sumUsageByApiKeyGroupedByUnitType(apiKeyId, from, to);
        BigDecimal totalCost = BigDecimal.ZERO;

        List<UsageSummaryItem> items = new ArrayList<>();
        for (Object[] row : rows) {
            UsageSummaryItem item = UsageSummaryItem.builder()
                    .usageUnitType((UsageUnitType) row[0])
                    .totalQuantity((BigDecimal) row[1])
                    .totalCost((BigDecimal) row[2])
                    .build();
            items.add(item);
            totalCost = totalCost.add(item.getTotalCost());
        }

        return UsageSummaryResponse.builder()
                .periodStart(from)
                .periodEnd(to)
                .totalCost(totalCost)
                .breakdown(items)
                .build();
    }

    /**
     * Get usage summary for an API key grouped by model and unit type.
     *
     * @param apiKeyId API key ID
     * @param from     period start
     * @param to       period end
     * @return usage summary with per-model breakdown
     */
    @Transactional(readOnly = true)
    public UsageSummaryResponse getUsageSummaryByApiKeyGroupedByModel(Integer apiKeyId, LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = usageRecordRepository.sumUsageByApiKeyGroupedByModelAndUnitType(apiKeyId, from, to);
        BigDecimal totalCost = BigDecimal.ZERO;

        List<UsageSummaryItem> items = new ArrayList<>();
        for (Object[] row : rows) {
            Integer modelId = (Integer) row[0];
            String modelName = modelRepository.findById(modelId)
                    .map(Model::getFullname)
                    .orElse("Unknown");

            UsageSummaryItem item = UsageSummaryItem.builder()
                    .modelId(modelId)
                    .modelName(modelName)
                    .usageUnitType((UsageUnitType) row[1])
                    .totalQuantity((BigDecimal) row[2])
                    .totalCost((BigDecimal) row[3])
                    .taskCount((Long) row[4])
                    .build();
            items.add(item);
            totalCost = totalCost.add(item.getTotalCost());
        }

        return UsageSummaryResponse.builder()
                .periodStart(from)
                .periodEnd(to)
                .totalCost(totalCost)
                .breakdown(items)
                .build();
    }

    /**
     * Get paginated usage history for an API key within a date range.
     *
     * @param apiKeyId API key ID
     * @param from     period start
     * @param to       period end
     * @param pageable pagination parameters
     * @return page of usage record responses
     */
    @Transactional(readOnly = true)
    public Page<UsageRecordResponse> getUsageHistory(Integer apiKeyId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return usageRecordRepository.findByApiKeyApiKeyIdAndRecordedAtBetween(apiKeyId, from, to, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get total cost for a user across all API keys within a date range.
     *
     * @param userId user ID
     * @param from   period start
     * @param to     period end
     * @return total cost
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalCostByUser(Integer userId, LocalDateTime from, LocalDateTime to) {
        return usageRecordRepository.sumCostByUserAndPeriod(userId, from, to);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalCostForAllUsers(LocalDateTime from, LocalDateTime to) {
        return usageRecordRepository.sumCostByPeriod(from, to);
    }

    @Transactional(readOnly = true)
    public UserDashboardInsightsResponse getUserDashboardInsights(Integer userId, BigDecimal currentBalance) {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        LocalDateTime thisMonthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay();

        long activeApiKeys = apiKeyRepository.countByUserUserIdAndStatus(userId, ApiKeyStatus.ACTIVE);
        long tasksThisMonth = taskRepository.countByApiKey_User_UserIdAndCreatedAtBetween(userId, thisMonthStart, now);
        BigDecimal todaysConsumption = taskRepository.sumTotalCostByUserAndStatusAndCompletedAtBetween(userId, TaskStatus.COMPLETED, todayStart, now);

        return UserDashboardInsightsResponse.builder()
            .currentBalance(currentBalance.setScale(2, RoundingMode.HALF_UP))
            .activeApiKeys(activeApiKeys)
            .tasksThisMonth(tasksThisMonth)
            .todaysConsumption(todaysConsumption.setScale(2, RoundingMode.HALF_UP))
            .build();
    }

    @Transactional(readOnly = true)
    public UserSpendingResponse getUserSpending(Integer userId, LocalDateTime from, LocalDateTime to, String filterPeriod) {
        BigDecimal totalSpending = taskRepository
            .sumTotalCostByUserAndStatusAndCompletedAtBetween(userId, TaskStatus.COMPLETED, from, to)
            .setScale(3, RoundingMode.HALF_UP);

        List<DailySpendingPoint> dailyTrend = new ArrayList<>();
        for (Object[] row : taskRepository.sumTotalCostByUserAndStatusGroupedByDateBetween(userId, TaskStatus.COMPLETED, from, to)) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            LocalDate date = toLocalDate(row[0]);
            BigDecimal amount = toBigDecimal(row[1]).setScale(3, RoundingMode.HALF_UP);
            dailyTrend.add(DailySpendingPoint.builder()
                .date(date.toString())
                .value(amount)
                .build());
        }

        return UserSpendingResponse.builder()
            .filterPeriod(filterPeriod)
            .fromDate(from)
            .toDate(to)
            .totalSpending(totalSpending)
            .dailyTrend(dailyTrend)
            .build();
    }

    @Transactional(readOnly = true)
    public UserUsageByModelTypeResponse getUserUsageByModelType(Integer userId, LocalDateTime from, LocalDateTime to, String filterPeriod) {
        return UserUsageByModelTypeResponse.builder()
            .usageByModelTypeCounts(getUsageByModelTypeCounts(userId, from, to))
            .build();
    }

    @Transactional(readOnly = true)
    public UserUsageByModelTypeResponse getUserUsageByModelTypeLifetime(Integer userId) {
        return UserUsageByModelTypeResponse.builder()
            .usageByModelTypeCounts(getUsageByModelTypeCountsLifetimeActive(userId))
            .build();
    }

        @Transactional(readOnly = true)
        public UserDashboardOverviewResponse getUserDashboardOverview(Integer userId, BigDecimal currentBalance) {
            LocalDateTime now = LocalDateTime.now(Clock.systemUTC());

        LocalDateTime thisMonthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        long activeApiKeys = apiKeyRepository.countByUserUserIdAndStatus(userId, ApiKeyStatus.ACTIVE);
        long tasksThisMonth = taskRepository.countByApiKey_User_UserIdAndCreatedAtBetween(userId, thisMonthStart, now);

        LocalDateTime todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay();
        BigDecimal todaysConsumption = taskRepository.sumTotalCostByUserAndStatusAndCompletedAtBetween(userId, TaskStatus.COMPLETED, todayStart, now);

        YearMonth currentMonth = YearMonth.from(now);
        List<MonthlySpendingPoint> trend = new ArrayList<>();
        BigDecimal spendingLast12Months = BigDecimal.ZERO;

        for (int i = 11; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            LocalDateTime monthStart = month.atDay(1).atStartOfDay();
            LocalDateTime monthEnd = month.equals(currentMonth)
                ? now
                : month.plusMonths(1).atDay(1).atStartOfDay().minusNanos(1);

            BigDecimal value = taskRepository
                .sumTotalCostByUserAndStatusAndCompletedAtBetween(userId, TaskStatus.COMPLETED, monthStart, monthEnd)
                .setScale(2, RoundingMode.HALF_UP);
            spendingLast12Months = spendingLast12Months.add(value);

                trend.add(MonthlySpendingPoint.builder()
                    .monthLabel(month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                .month(month.getMonthValue())
                .year(month.getYear())
                .value(value)
                .build());
        }

        return UserDashboardOverviewResponse.builder()
            .currentBalance(currentBalance.setScale(2, RoundingMode.HALF_UP))
            .activeApiKeys(activeApiKeys)
            .tasksThisMonth(tasksThisMonth)
            .todaysConsumption(todaysConsumption.setScale(2, RoundingMode.HALF_UP))
            .spendingLast12Months(spendingLast12Months.setScale(2, RoundingMode.HALF_UP))
            .monthlySpendingTrend(trend)
            .build();
        }

    @Transactional(readOnly = true)
    public UserBillingInsightsResponse getUserBillingInsights(Integer userId, BigDecimal currentBalance) {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());

        LocalDateTime thisMonthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime lastMonthStart = thisMonthStart.minusMonths(1);
        LocalDateTime comparableLastMonthEnd = lastMonthStart.plusSeconds(java.time.Duration.between(thisMonthStart, now).getSeconds());

        BigDecimal creditsUsedThisMonth = taskRepository.sumTotalCostByUserAndStatusAndCompletedAtBetween(userId, TaskStatus.COMPLETED, thisMonthStart, now);
        BigDecimal creditsUsedComparableLastMonth = taskRepository.sumTotalCostByUserAndStatusAndCompletedAtBetween(userId, TaskStatus.COMPLETED, lastMonthStart, comparableLastMonthEnd);

        PeriodRange selectedRange = resolvePeriodRange("30days", now);
        BigDecimal totalSpend = taskRepository.sumTotalCostByUserAndStatusAndCompletedAtBetween(
                userId,
            TaskStatus.COMPLETED,
            selectedRange.from(),
            selectedRange.to()
        );
        long totalRequestsCurrentPeriod = taskRepository.countByApiKey_User_UserIdAndStatusAndCompletedAtBetween(
            userId,
            TaskStatus.COMPLETED,
            selectedRange.from(),
            selectedRange.to()
        );
        BigDecimal avgCostPerRequest = calculateAverage(totalSpend, totalRequestsCurrentPeriod);

        return UserBillingInsightsResponse.builder()
                .currentBalance(currentBalance.setScale(2, RoundingMode.HALF_UP))
                .creditsUsedThisMonth(creditsUsedThisMonth.setScale(2, RoundingMode.HALF_UP))
                .creditsUsedChangeFromLastMonthPercent(calculatePercentChange(creditsUsedThisMonth, creditsUsedComparableLastMonth))
            .totalSpend(totalSpend.setScale(2, RoundingMode.HALF_UP))
                .avgCostPerRequest(avgCostPerRequest)
                .build();
    }

    @Transactional(readOnly = true)
    public UserUsageInsightsResponse getUserUsageInsights(Integer userId) {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        PeriodRange current = resolvePeriodRange("30days", now);
        return getUserUsageInsights(userId, current.normalizedPeriod(), current.from(), current.to());
    }

    @Transactional(readOnly = true)
    public UserUsageInsightsResponse getUserUsageInsights(Integer userId,
                                                          String period,
                                                          LocalDateTime from,
                                                          LocalDateTime to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before to");
        }

        long seconds = Math.max(1L, java.time.Duration.between(from, to).getSeconds());
        LocalDateTime previousEnd = from.minusSeconds(1);
        LocalDateTime previousStart = previousEnd.minusSeconds(seconds);

        BigDecimal totalSpend = usageRecordRepository.sumCostByUserAndPeriod(userId, from, to);
        BigDecimal priorSpend = usageRecordRepository.sumCostByUserAndPeriod(userId, previousStart, previousEnd);

        long totalRequests = taskRepository.countByApiKey_User_UserIdAndStatusAndCompletedAtBetween(
            userId,
            TaskStatus.COMPLETED,
            from,
            to);
        long priorRequests = taskRepository.countByApiKey_User_UserIdAndStatusAndCompletedAtBetween(
            userId,
            TaskStatus.COMPLETED,
            previousStart,
            previousEnd);

        BigDecimal avgCostPerRequest = calculateAverage(totalSpend, totalRequests);
        BigDecimal priorAvgCostPerRequest = calculateAverage(priorSpend, priorRequests);

        BigDecimal totalSpendChangePercent = calculatePercentChange(totalSpend, priorSpend);
        BigDecimal totalRequestsChangePercent = calculatePercentChange(BigDecimal.valueOf(totalRequests), BigDecimal.valueOf(priorRequests));
        BigDecimal avgCostPerRequestChangePercent = calculatePercentChange(avgCostPerRequest, priorAvgCostPerRequest);

        List<Object[]> topModels = usageRecordRepository.findTopModelsByUserAndPeriod(
            userId,
            from,
            to,
            PageRequest.of(0, 1)
        );

        String mostUsedModel = topModels.isEmpty() ? "N/A" : String.valueOf(topModels.get(0)[0]);

        return UserUsageInsightsResponse.builder()
            .totalSpend(totalSpend.setScale(2, RoundingMode.HALF_UP))
            .totalSpendChangePercent(totalSpendChangePercent)
            .totalRequests(totalRequests)
            .totalRequestsChangePercent(totalRequestsChangePercent)
            .mostUsedModel(mostUsedModel)
            .avgCostPerRequest(avgCostPerRequest)
            .avgCostPerRequestChangePercent(avgCostPerRequestChangePercent)
            .build();
    }

    @Transactional(readOnly = true)
    public UserUsageHistoryResponse getUserUsageHistory(Integer userId,
                                                        String period,
                                                        LocalDateTime from,
                                                        LocalDateTime to) {
        List<Object[]> unitRows = usageRecordRepository.sumUsageByUserGroupedByDateAndUnitTypeAndPeriod(userId, from, to);
        List<Object[]> requestRows = taskRepository.countByUserAndStatusGroupedByCompletedDateBetween(
            userId,
            TaskStatus.COMPLETED,
            from,
            to);

        Map<LocalDate, Long> requestCountByDate = new HashMap<>();
        for (Object[] row : requestRows) {
            LocalDate date = toLocalDate(row[0]);
            long count = row[1] == null ? 0L : ((Number) row[1]).longValue();
            requestCountByDate.put(date, count);
        }

        Map<LocalDate, UserUsageHistoryResponse.DailyUsageHistoryDay> dayMap = new LinkedHashMap<>();
        LocalDate startDate = from.toLocalDate();
        LocalDate endDate = to.toLocalDate();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dayMap.put(date, UserUsageHistoryResponse.DailyUsageHistoryDay.builder()
                    .date(date.toString())
                    .totalCost(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP))
                    .totalRequests(requestCountByDate.getOrDefault(date, 0L))
                    .usageByUnit(new LinkedHashMap<>())
                    .build());
        }

        for (Object[] row : unitRows) {
            LocalDate date = toLocalDate(row[0]);
            UsageUnitType unitType = (UsageUnitType) row[1];
            BigDecimal quantity = toBigDecimal(row[2]);
            BigDecimal cost = toBigDecimal(row[3]);

            UserUsageHistoryResponse.DailyUsageHistoryDay day = dayMap.get(date);
            if (day == null) {
            continue;
            }

            Map<String, UserUsageHistoryResponse.UnitUsageSummary> usageByUnit = day.getUsageByUnit();

            BigDecimal avgRate = quantity.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : cost.divide(quantity, 6, RoundingMode.HALF_UP);

            usageByUnit.put(unitType.name(), UserUsageHistoryResponse.UnitUsageSummary.builder()
                .quantity(quantity.setScale(4, RoundingMode.HALF_UP))
                .totalCost(cost.setScale(6, RoundingMode.HALF_UP))
                .avgRatePerUnit(avgRate)
                .build());

                day.setTotalCost(day.getTotalCost().add(cost).setScale(3, RoundingMode.HALF_UP));
                day.setUsageByUnit(usageByUnit);
        }

            List<UserUsageHistoryResponse.DailyUsageHistoryDay> dailyHistory = new ArrayList<>(dayMap.values());

        return UserUsageHistoryResponse.builder()
            .period(period)
            .fromDate(from)
            .toDate(to)
            .dailyHistory(dailyHistory)
            .build();
    }

    // ---- Internal helpers ----

    private BigDecimal calculateAverage(BigDecimal total, long count) {
        if (count <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private LocalDate toLocalDate(Object rawDate) {
        if (rawDate instanceof LocalDate localDate) {
            return localDate;
        }
        if (rawDate instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        if (rawDate instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return LocalDate.parse(rawDate.toString());
    }

    private BigDecimal toBigDecimal(Object rawValue) {
        if (rawValue == null) {
            return BigDecimal.ZERO;
        }
        if (rawValue instanceof BigDecimal value) {
            return value;
        }
        if (rawValue instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(rawValue.toString());
    }

    private BigDecimal calculatePercentChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }

    private PeriodRange resolvePeriodRange(String period, LocalDateTime now) {
        String normalized = normalizePeriod(period);
        LocalDateTime from;

        switch (normalized) {
            case "7days" -> from = now.minusDays(7);
            case "15days" -> from = now.minusDays(15);
            case "30days" -> from = now.minusDays(30);
            case "week" -> from = now.minusWeeks(1);
            case "3m" -> from = now.minusMonths(3);
            case "6m" -> from = now.minusMonths(6);
            case "year" -> from = now.minusYears(1);
            default -> from = now.minusDays(30);
        }

        return new PeriodRange(normalized, from, now);
    }

    private String normalizePeriod(String period) {
        if (period == null || period.isBlank()) {
            return "month";
        }

        String value = period.trim().toLowerCase();
        return switch (value) {
            case "7days", "7day", "week" -> "7days";
            case "15days", "15day" -> "15days";
            case "30days", "30day", "month", "1m" -> "30days";
            case "3m", "3month", "3months" -> "3m";
            case "6m", "6month", "6months" -> "6m";
            case "year", "1y", "12m" -> "year";
            default -> "30days";
        };
    }

    private Map<String, Long> getUsageByModelTypeCounts(Integer userId, LocalDateTime from, LocalDateTime to) {
        Map<ModelType, Long> rawCounts = new EnumMap<>(ModelType.class);
        for (Object[] row : taskRepository.countByUserGroupedByModelTypeAndCreatedAtBetween(userId, TaskStatus.COMPLETED, from, to)) {
            ModelType type = (ModelType) row[0];
            Number countNumber = (Number) row[1];
            rawCounts.put(type, countNumber.longValue());
        }

        Map<String, Long> counts = new HashMap<>();
        counts.put(ModelType.LLM.name(), rawCounts.getOrDefault(ModelType.LLM, 0L));
        counts.put(ModelType.OCR.name(), rawCounts.getOrDefault(ModelType.OCR, 0L));
        counts.put(ModelType.OTHER.name(), rawCounts.getOrDefault(ModelType.OTHER, 0L));
        return counts;
    }

    private Map<String, Long> getUsageByModelTypeCountsLifetimeActive(Integer userId) {
        Map<ModelType, Long> rawCounts = new EnumMap<>(ModelType.class);
        for (Object[] row : taskRepository.countByUserGroupedByModelTypeAndActiveApiKeyStatus(userId, ApiKeyStatus.ACTIVE, TaskStatus.COMPLETED)) {
            ModelType type = (ModelType) row[0];
            Number countNumber = (Number) row[1];
            rawCounts.put(type, countNumber.longValue());
        }

        Map<String, Long> counts = new HashMap<>();
        counts.put(ModelType.LLM.name(), rawCounts.getOrDefault(ModelType.LLM, 0L));
        counts.put(ModelType.OCR.name(), rawCounts.getOrDefault(ModelType.OCR, 0L));
        counts.put(ModelType.OTHER.name(), rawCounts.getOrDefault(ModelType.OTHER, 0L));
        return counts;
    }

    private BigDecimal extractRate(BillingConfig billingConfig, UsageUnitType unitType) {
        String metadata = billingConfig.getPricingMetadata();
        if (metadata == null || metadata.isBlank()) {
            throw new BillingCalculationException(
                "No pricing metadata found for billing config ID " + billingConfig.getBillingId());
        }

        try {
            JsonNode node = objectMapper.readTree(metadata);

            // Try unit-type-specific keys first
            String specificKey = getSpecificRateKey(unitType);
            if (specificKey != null && node.has(specificKey)) {
                return new BigDecimal(node.get(specificKey).asText());
            }

            // Fallback to generic "rate"
            if (node.has("rate")) {
                return new BigDecimal(node.get("rate").asText());
            }

            throw new BillingCalculationException(
                "No rate found in pricing metadata for unit type " + unitType
                + " in billing config ID " + billingConfig.getBillingId());

        } catch (JsonProcessingException e) {
            throw new BillingCalculationException(
                "Failed to parse pricing metadata for billing config ID " + billingConfig.getBillingId(), e);
        }
    }

    // Maps UsageUnitType → JSON key in pricingMetadata. Null = fallback to "rate".
    private String getSpecificRateKey(UsageUnitType unitType) {
        return switch (unitType) {
            case INPUT_TOKENS -> "inputRate";
            case OUTPUT_TOKENS -> "outputRate";
            case PAGES -> "pageRate";
            case IMAGES -> "imageRate";
            case AUDIO_SECONDS -> "secondRate";
            case REQUESTS -> "requestRate";
            case CHARACTERS -> "charRate";
            case EMBEDDING_TOKENS -> "embeddingRate";
            case CUSTOM_UNITS -> null;  // always falls back to generic "rate"
        };
    }

    private void updateTaskCost(Task task) {
        List<UsageRecord> records = usageRecordRepository.findByTask(task);
        BigDecimal total = records.stream()
                .map(UsageRecord::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        task.setTotalCost(total);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
    }

    private UsageRecordResponse mapToResponse(UsageRecord record) {
        return UsageRecordResponse.builder()
                .usageId(record.getUsageId())
                .taskId(record.getTask().getTaskId())
                .modelId(record.getModel().getModelId())
                .modelName(record.getModel().getFullname())
                .apiKeyId(record.getApiKey().getApiKeyId())
                .usageUnitType(record.getUsageUnitType())
                .quantity(record.getQuantity())
                .ratePerUnit(record.getRatePerUnit())
                .cost(record.getCost())
                .recordedAt(record.getRecordedAt())
                .build();
    }

    private void checkAndNotifyMonthlyApiKeyUsage(com.fleebug.corerouter.entity.apikey.ApiKey apiKey) {
        if (apiKey == null || apiKey.getApiKeyId() == null || apiKey.getMonthlyLimit() == null || apiKey.getMonthlyLimit() <= 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        long consumed = usageRecordRepository.countByApiKeyApiKeyIdAndRecordedAtBetween(apiKey.getApiKeyId(), monthStart, now);
        int monthlyLimit = apiKey.getMonthlyLimit();
        int percentConsumed = (int) Math.floor((consumed * 100.0) / monthlyLimit);

        String monthKey = YearMonth.now().toString();
        if (consumed >= monthlyLimit) {
            maybeQueueMonthlyUsageAlert(apiKey, consumed, monthlyLimit, percentConsumed, monthKey, 100);
        }
        
        else if (percentConsumed >= 90) {
            maybeQueueMonthlyUsageAlert(apiKey, consumed, monthlyLimit, percentConsumed, monthKey, 90);
        }
        else if (percentConsumed >= 80) {
            maybeQueueMonthlyUsageAlert(apiKey, consumed, monthlyLimit, percentConsumed, monthKey, 80);
        }
        
    }

    private void maybeQueueMonthlyUsageAlert(com.fleebug.corerouter.entity.apikey.ApiKey apiKey,
                                             long consumed,
                                             int monthlyLimit,
                                             int percentConsumed,
                                             String monthKey,
                                             int thresholdPercent) {
        String dedupeKey = API_KEY_MONTHLY_ALERT_PREFIX + apiKey.getApiKeyId() + ":" + monthKey + ":" + thresholdPercent;
        if (redisService.existsInCache(dedupeKey)) {
            return;
        }

        redisService.saveToCache(dedupeKey, "true", 40, TimeUnit.DAYS);
        otpService.publishApiKeyMonthlyUsageAlert(
                apiKey.getUser().getEmail(),
                apiKey.getUser().getFullName(),
            apiKey.getUser().getUserId(),
                apiKey.getApiKeyId(),
            apiKey.getDescription(),
                monthlyLimit,
                consumed,
                thresholdPercent == 100 ? 100 : percentConsumed
        );
    }

    private record PeriodRange(String normalizedPeriod, LocalDateTime from, LocalDateTime to) {
    }
}
