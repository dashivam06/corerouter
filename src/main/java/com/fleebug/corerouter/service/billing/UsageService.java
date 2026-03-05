package com.fleebug.corerouter.service.billing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleebug.corerouter.dto.billing.request.RecordUsageRequest;
import com.fleebug.corerouter.dto.billing.response.UsageRecordResponse;
import com.fleebug.corerouter.dto.billing.response.UsageSummaryItem;
import com.fleebug.corerouter.dto.billing.response.UsageSummaryResponse;
import com.fleebug.corerouter.entity.billing.BillingConfig;
import com.fleebug.corerouter.entity.billing.UsageRecord;
import com.fleebug.corerouter.entity.model.Model;
import com.fleebug.corerouter.entity.task.Task;
import com.fleebug.corerouter.enums.billing.UsageUnitType;
import com.fleebug.corerouter.exception.billing.BillingCalculationException;
import com.fleebug.corerouter.exception.task.TaskNotFoundException;
import com.fleebug.corerouter.repository.billing.UsageRecordRepository;
import com.fleebug.corerouter.repository.model.ModelRepository;
import com.fleebug.corerouter.repository.task.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UsageService {

    private final UsageRecordRepository usageRecordRepository;
    private final TaskRepository taskRepository;
    private final ModelRepository modelRepository;
    private final BillingConfigService billingConfigService;
    private final ObjectMapper objectMapper;

    /**
     * Record usage for a task, compute cost from billing config, and update task total cost.
     *
     * @param request usage recording request
     * @return recorded usage response
     */
    public UsageRecordResponse recordUsage(RecordUsageRequest request) {
        log.info("Recording usage for taskId={}, unitType={}, quantity={}",
                request.getTaskId(), request.getUsageUnitType(), request.getQuantity());

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
        log.info("Usage recorded: usageId={}, taskId={}, cost={}", saved.getUsageId(), request.getTaskId(), cost);

        // Update task totalCost
        updateTaskCost(task);

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

    // ---- Internal helpers ----

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
}
