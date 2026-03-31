package com.fleebug.corerouter.service.health;

import com.fleebug.corerouter.config.AzureTokenProvider;
import com.fleebug.corerouter.exception.health.AzureInsightsAccessException;
import com.fleebug.corerouter.util.HttpClientUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AzureInsightsService {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int DEFAULT_LOG_PAGE_SIZE = 100;
    private static final int MAX_PAGE_SIZE = 200;
    private static final String DEFAULT_RANGE_KEY = "1d";

    @Value("${azure.appinsights.app.id}")
    private String appId;

    private final HttpClientUtil httpClientUtil;
    private final AzureTokenProvider azureTokenProvider;

    private Object query(String kql) {
        String endpoint = "https://api.applicationinsights.io/v1/apps/" + appId + "/query";
        String token = azureTokenProvider.getToken();
        try {
            return httpClientUtil.postJsonForObject(
                endpoint,
                Map.of(
                    "Authorization", "Bearer " + token,
                    "Content-Type", "application/json"
                ),
                Map.of("query", kql),
                Object.class,
                5000,
                10000
            );
        } catch (IllegalStateException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage();
            if (message.contains("status 403") || message.contains("status 401") || message.contains("InsufficientAccessError")) {
                throw new AzureInsightsAccessException(
                        "Azure Application Insights access denied. Verify Entra app permissions/RBAC for this Application Insights resource.",
                        ex
                );
            }
            throw ex;
        }
    }

    public Object getWarningsPaged(LocalDateTime from, LocalDateTime to, Integer pageSize,
                                   LocalDateTime cursorTimestamp, String cursorItemId) {
        LocalDateTime resolvedFrom = resolveFrom(from, to);
        LocalDateTime resolvedTo = resolveTo(from, to);
        int resolvedPageSize = sanitizePageSize(pageSize, DEFAULT_PAGE_SIZE);

        String cursorClause = buildCursorClause(cursorTimestamp, cursorItemId);
        String kql = """
                traces
                | where timestamp between (datetime(%s) .. datetime(%s))
                | where severityLevel == 2
                %s
                | project
                    timestamp,
                    service = cloud_RoleName,
                    instanceId = cloud_RoleInstance,
                    message,
                    severity = "WARN",
                    itemId = tostring(itemId)
                | order by timestamp desc, itemId desc
                | take %d
                """.formatted(formatDate(resolvedFrom), formatDate(resolvedTo), cursorClause, resolvedPageSize);

        String countKql = """
                traces
                | where timestamp between (datetime(%s) .. datetime(%s))
                | where severityLevel == 2
                | summarize totalCount = count()
                """.formatted(formatDate(resolvedFrom), formatDate(resolvedTo));

        return buildPagedResponse(query(kql), query(countKql), resolvedFrom, resolvedTo, resolvedPageSize);
    }

    public Object getErrors(LocalDateTime from, LocalDateTime to) {
        return getErrorsPaged(from, to, DEFAULT_PAGE_SIZE, null, null);
    }

    public Object getErrorsPaged(LocalDateTime from, LocalDateTime to, Integer pageSize,
                                 LocalDateTime cursorTimestamp, String cursorItemId) {
        LocalDateTime resolvedFrom = resolveFrom(from, to);
        LocalDateTime resolvedTo = resolveTo(from, to);
        int resolvedPageSize = sanitizePageSize(pageSize, DEFAULT_PAGE_SIZE);

        String cursorClause = buildCursorClause(cursorTimestamp, cursorItemId);
        String kql = """
                exceptions
                | where timestamp between (datetime(%s) .. datetime(%s))
                %s
                | project
                    timestamp,
                    service = cloud_RoleName,
                    instanceId = cloud_RoleInstance,
                    message = outerMessage,
                    detail = innermostMessage,
                    severity = "ERROR",
                    itemId = tostring(itemId)
                | order by timestamp desc, itemId desc
                | take %d
                """.formatted(formatDate(resolvedFrom), formatDate(resolvedTo), cursorClause, resolvedPageSize);

        String countKql = """
                exceptions
                | where timestamp between (datetime(%s) .. datetime(%s))
                | summarize totalCount = count()
                """.formatted(formatDate(resolvedFrom), formatDate(resolvedTo));

        return buildPagedResponse(query(kql), query(countKql), resolvedFrom, resolvedTo, resolvedPageSize);
    }

    public Object getWarnings(LocalDateTime from, LocalDateTime to) {
        return getWarningsPaged(from, to, DEFAULT_PAGE_SIZE, null, null);
    }

    public Object getLogs(LocalDateTime from, LocalDateTime to, String severity) {
        return getLogsPaged(from, to, severity, DEFAULT_LOG_PAGE_SIZE, null, null);
    }

    public Object getLogsPaged(LocalDateTime from, LocalDateTime to, String severity, Integer pageSize,
                               LocalDateTime cursorTimestamp, String cursorItemId) {
        LocalDateTime resolvedFrom = resolveFrom(from, to);
        LocalDateTime resolvedTo = resolveTo(from, to);
        int resolvedPageSize = sanitizePageSize(pageSize, DEFAULT_LOG_PAGE_SIZE);

        String normalizedSeverity = severity == null ? "ALL" : severity.trim().toUpperCase(Locale.ROOT);
        int severityLevel = mapSeverity(normalizedSeverity);
        String severityClause = "ALL".equals(normalizedSeverity) ? "" : "| where severityLevel == " + severityLevel;
        String cursorClause = buildCursorClause(cursorTimestamp, cursorItemId);

        String kql = """
                union traces, exceptions
                | where timestamp between (datetime(%s) .. datetime(%s))
                %s
                %s
                | project
                    timestamp,
                    service = cloud_RoleName,
                    instanceId = cloud_RoleInstance,
                    message = coalesce(message, outerMessage),
                    itemId = tostring(itemId),
                    severity = case(
                        severityLevel == 4, "ERROR",
                        severityLevel == 2, "WARN",
                        severityLevel == 1, "INFO",
                        "UNKNOWN"
                    )
                | order by timestamp desc, itemId desc
                | take %d
                """.formatted(
                formatDate(resolvedFrom),
                formatDate(resolvedTo),
                severityClause,
                cursorClause,
                resolvedPageSize
        );

        String countKql = """
                union traces, exceptions
                | where timestamp between (datetime(%s) .. datetime(%s))
                %s
                | summarize totalCount = count()
                """.formatted(formatDate(resolvedFrom), formatDate(resolvedTo), severityClause);

        return buildPagedResponse(query(kql), query(countKql), resolvedFrom, resolvedTo, resolvedPageSize);
    }

    public Object getRequestStats(LocalDateTime from, LocalDateTime to) {
        String kql = """
                requests
                | where timestamp between (datetime(%s) .. datetime(%s))
                | summarize
                    TotalCalls = count(),
                    SuccessCalls = countif(success == true),
                    FailedCalls = countif(success == false),
                    SuccessRate = round(100.0 * countif(success == true) / count(), 2),
                    AvgDuration = round(avg(duration), 0),
                    P95Duration = round(percentile(duration, 95), 0)
                """.formatted(formatDate(resolveFrom(from, to)), formatDate(resolveTo(from, to)));
        return query(kql);
    }

    public Object getTotalRequests(String rangeKey) {
        RangeConfig range = resolveRange(rangeKey);
        String kql = """
                requests
                | where timestamp >= ago(%s)
                %s
                | summarize totalRequests = count()
                """.formatted(range.range(), genuineUserFilterClause());
        return query(kql);
    }

    public Object getFailedRequests(String rangeKey) {
        RangeConfig range = resolveRange(rangeKey);
        String kql = """
                requests
                | where timestamp >= ago(%s)
                %s
                | summarize failedRequests = countif(success == false)
                """.formatted(range.range(), genuineUserFilterClause());
        return query(kql);
    }

    public Object getErrorRate(String rangeKey) {
        RangeConfig range = resolveRange(rangeKey);
        String kql = """
                requests
                | where timestamp >= ago(%s)
                %s
                | summarize total = count(), failed = countif(success == false)
                | extend errorRate = iff(total == 0, 0.0, failed * 100.0 / total)
                """.formatted(range.range(), genuineUserFilterClause());
        return query(kql);
    }

    public Object getAverageResponseTime(String rangeKey) {
        RangeConfig range = resolveRange(rangeKey);
        String kql = """
                requests
                | where timestamp >= ago(%s)
                %s
                | summarize avgDurationMs = round(avg(duration / 1ms), 2)
                """.formatted(range.range(), genuineUserFilterClause());
        return query(kql);
    }

    public Object getRequestsOverTime(String rangeKey) {
        RangeConfig range = resolveRange(rangeKey);
        String kql = """
                requests
                | where timestamp >= ago(%s)
                %s
                | summarize requests = count() by bin(timestamp, %s)
                | order by timestamp asc
                """.formatted(range.range(), genuineUserFilterClause(), range.interval());
        return query(kql);
    }

    public Object getFailedRequestsOverTime(String rangeKey) {
        RangeConfig range = resolveRange(rangeKey);
        String kql = """
                requests
                | where timestamp >= ago(%s)
                %s
                | where success == false
                | summarize failures = count() by bin(timestamp, %s)
                | order by timestamp asc
                """.formatted(range.range(), genuineUserFilterClause(), range.interval());
        return query(kql);
    }

    public Object getTopEndpointsForRange(String rangeKey) {
        RangeConfig range = resolveRange(rangeKey);
        String kql = """
                requests
                | where timestamp >= ago(%s)
                %s
                | summarize requestCount = count() by name
                | top 5 by requestCount desc
                """.formatted(range.range(), genuineUserFilterClause());
        return query(kql);
    }

    private String genuineUserFilterClause() {
        return """
                | where not(name has "AdminTechnicalController")
                | where not(name has "InternalWorkerController")
                | where not(tostring(url) has "/api/v1/admin/")
                | where not(tostring(url) has "/api/v1/internal/")
                """;
    }

    public Object getFailedJobs(LocalDateTime from, LocalDateTime to) {
        return getFailedJobsPaged(from, to, DEFAULT_PAGE_SIZE, null, null);
    }

    public Object getFailedJobsPaged(LocalDateTime from, LocalDateTime to, Integer pageSize,
                                     LocalDateTime cursorTimestamp, String cursorItemId) {
        LocalDateTime resolvedFrom = resolveFrom(from, to);
        LocalDateTime resolvedTo = resolveTo(from, to);
        int resolvedPageSize = sanitizePageSize(pageSize, DEFAULT_PAGE_SIZE);

        String cursorClause = buildCursorClause(cursorTimestamp, cursorItemId);
        String kql = """
                customEvents
                | where name == "JOB_FAILED"
                | where timestamp between (datetime(%s) .. datetime(%s))
                %s
                | project
                    timestamp,
                    jobId = tostring(customDimensions.jobId),
                    jobType = tostring(customDimensions.jobType),
                    reason = tostring(customDimensions.reason),
                    instanceId = tostring(customDimensions.instanceId),
                    retryCount = toint(customMeasurements.retryCount),
                    itemId = tostring(itemId)
                | order by timestamp desc, itemId desc
                | take %d
                """.formatted(formatDate(resolvedFrom), formatDate(resolvedTo), cursorClause, resolvedPageSize);

        String countKql = """
                customEvents
                | where name == "JOB_FAILED"
                | where timestamp between (datetime(%s) .. datetime(%s))
                | summarize totalCount = count()
                """.formatted(formatDate(resolvedFrom), formatDate(resolvedTo));

        return buildPagedResponse(query(kql), query(countKql), resolvedFrom, resolvedTo, resolvedPageSize);
    }

    public Object getTopEndpoints(LocalDateTime from, LocalDateTime to) {
        String kql = """
                requests
                | where timestamp between (datetime(%s) .. datetime(%s))
                | summarize
                    Calls = count(),
                    Failed = countif(success == false),
                    AvgDuration = round(avg(duration), 0)
                  by name
                | order by Calls desc
                | take 10
                """.formatted(formatDate(resolveFrom(from, to)), formatDate(resolveTo(from, to)));
        return query(kql);
    }

    public Object getTrafficByHour(LocalDateTime from, LocalDateTime to) {
        String kql = """
                requests
                | where timestamp between (datetime(%s) .. datetime(%s))
                | summarize
                    Calls = count(),
                    Failed = countif(success == false)
                  by bin(timestamp, 1h)
                | order by timestamp asc
                """.formatted(formatDate(resolveFrom(from, to)), formatDate(resolveTo(from, to)));
        return query(kql);
    }

    public Object getAlerts(LocalDateTime from, LocalDateTime to) {
        String kql = """
                union
                (
                    requests
                    | where timestamp between (datetime(%s) .. datetime(%s))
                    | summarize
                        Total = count(),
                        Failed = countif(success == false)
                      by cloud_RoleName
                    | where (Failed * 100.0 / Total) > 5
                    | project
                        service = cloud_RoleName,
                        alertType = "HIGH_ERROR_RATE",
                        severity = "WARN",
                        value = strcat(tostring(round(Failed * 100.0 / Total, 2)), "%%")
                ),
                (
                    requests
                    | where timestamp between (datetime(%s) .. datetime(%s))
                    | summarize P95 = percentile(duration, 95) by cloud_RoleName
                    | where P95 > 2000
                    | project
                        service = cloud_RoleName,
                        alertType = "SLOW_RESPONSE",
                        severity = "WARN",
                        value = strcat(tostring(round(P95, 0)), "ms")
                ),
                (
                    customEvents
                    | where name in ("WORKER_DOWN", "VLLM_DOWN", "REDIS_DOWN")
                    | where timestamp between (datetime(%s) .. datetime(%s))
                    | project
                        service = cloud_RoleName,
                        alertType = name,
                        severity = "HIGH",
                        value = tostring(customDimensions.reason)
                )
                | order by severity desc
                """.formatted(
                formatDate(resolveFrom(from, to)), formatDate(resolveTo(from, to)),
                formatDate(resolveFrom(from, to)), formatDate(resolveTo(from, to)),
                formatDate(resolveFrom(from, to)), formatDate(resolveTo(from, to))
        );

        return query(kql);
    }

    private int mapSeverity(String severity) {
        return switch (severity) {
            case "ERROR" -> 4;
            case "WARN" -> 2;
            case "INFO" -> 1;
            default -> 0;
        };
    }

    private RangeConfig resolveRange(String rangeKey) {
        String key = rangeKey == null ? DEFAULT_RANGE_KEY : rangeKey.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "1d" -> new RangeConfig("1d", "5m");
            case "3d" -> new RangeConfig("3d", "15m");
            case "7d" -> new RangeConfig("7d", "1h");
            case "1m" -> new RangeConfig("30d", "6h");
            case "3m" -> new RangeConfig("90d", "1d");
            case "6m" -> new RangeConfig("180d", "1d");
            default -> new RangeConfig("1d", "5m");
        };
    }

    private int sanitizePageSize(Integer requestedPageSize, int defaultSize) {
        int size = requestedPageSize == null ? defaultSize : requestedPageSize;
        if (size < 1) {
            return defaultSize;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String buildCursorClause(LocalDateTime cursorTimestamp, String cursorItemId) {
        if (cursorTimestamp == null) {
            return "";
        }

        String ts = formatDate(cursorTimestamp);
        if (cursorItemId == null || cursorItemId.isBlank()) {
            return "| where timestamp < datetime(" + ts + ")";
        }

        String escapedItemId = cursorItemId.replace("\"", "\\\""); // like man for safety only, since itemId is generally guid style we still have to have some precaution for any special characters
        return "| where timestamp < datetime(" + ts + ") or (timestamp == datetime(" + ts + ") and tostring(itemId) < \"" + escapedItemId + "\")";
    }

    private List<Map<String, Object>> extractItems(Object queryResponse) {
        if (!(queryResponse instanceof Map<?, ?> responseMap)) {
            return List.of();
        }

        Object tablesObject = responseMap.get("tables");
        if (!(tablesObject instanceof List<?> tables) || tables.isEmpty()) {
            return List.of();
        }

        Object firstTableObject = tables.get(0);
        if (!(firstTableObject instanceof Map<?, ?> firstTable)) {
            return List.of();
        }

        Object columnsObject = firstTable.get("columns");
        Object rowsObject = firstTable.get("rows");
        if (!(columnsObject instanceof List<?> columns) || !(rowsObject instanceof List<?> rows)) {
            return List.of();
        }

        List<String> columnNames = new ArrayList<>();
        for (Object column : columns) {
            if (column instanceof Map<?, ?> columnMap) {
                Object name = columnMap.get("name");
                columnNames.add(name == null ? "" : String.valueOf(name));
            }
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (Object rowObj : rows) {
            if (!(rowObj instanceof List<?> row)) {
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            for (int i = 0; i < row.size() && i < columnNames.size(); i++) {
                item.put(columnNames.get(i), row.get(i));
            }
            items.add(item);
        }

        return items;
    }

    private long extractTotalCount(Object countResponse) {
        List<Map<String, Object>> countRows = extractItems(countResponse);
        if (countRows.isEmpty()) {
            return 0;
        }

        Object total = countRows.get(0).get("totalCount");
        if (total instanceof Number number) {
            return number.longValue();
        }

        try {
            return total == null ? 0 : Long.parseLong(String.valueOf(total));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private Map<String, Object> buildPagedResponse(Object pageResponse,
                                                   Object countResponse,
                                                   LocalDateTime from,
                                                   LocalDateTime to,
                                                   int pageSize) {
        List<Map<String, Object>> items = extractItems(pageResponse);
        long totalCount = extractTotalCount(countResponse);
        long totalPages = pageSize == 0 ? 0 : (long) Math.ceil((double) totalCount / pageSize);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("from", formatDate(from));
        response.put("to", formatDate(to));
        response.put("pageSize", pageSize);
        response.put("countInPage", items.size());
        response.put("totalCount", totalCount);
        response.put("totalPages", totalPages);
        response.put("items", items);

        if (!items.isEmpty()) {
            Map<String, Object> lastItem = items.get(items.size() - 1);
            response.put("nextCursorTimestamp", lastItem.get("timestamp"));
            response.put("nextCursorItemId", lastItem.get("itemId"));
        } else {
            response.put("nextCursorTimestamp", null);
            response.put("nextCursorItemId", null);
        }

        return response;
    }

    private LocalDateTime resolveFrom(LocalDateTime from, LocalDateTime to) {
        if (from != null) {
            return from;
        }
        if (to != null) {
            return to.minusHours(24);
        }
        return LocalDateTime.now().minusHours(24);
    }

    private LocalDateTime resolveTo(LocalDateTime from, LocalDateTime to) {
        if (to != null) {
            return to;
        }
        if (from != null) {
            return from.plusHours(24);
        }
        return LocalDateTime.now();
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private record RangeConfig(String range, String interval) {}
}
