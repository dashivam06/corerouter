package com.fleebug.corerouter.service.billing;

import com.fleebug.corerouter.entity.billing.BillingConfig;
import com.fleebug.corerouter.entity.billing.UsageRecord;
import com.fleebug.corerouter.entity.task.Task;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.task.TaskStatus;
import com.fleebug.corerouter.exception.billing.BillingCalculationException;
import com.fleebug.corerouter.repository.billing.BillingConfigRepository;
import com.fleebug.corerouter.repository.billing.UsageRecordRepository;
import com.fleebug.corerouter.repository.task.TaskRepository;
import com.fleebug.corerouter.repository.user.UserRepository;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskBillingService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final BillingConfigRepository billingConfigRepository;
    private final TelemetryClient telemetryClient;

    public void applyDebitIfEligible(Task task) {
        if (task == null || task.getStatus() != TaskStatus.COMPLETED) {
            return;
        }

        BigDecimal targetChargedCost = calculateTargetChargedCost(task);
        BigDecimal chargedCost = task.getChargedCost() == null ? BigDecimal.ZERO : task.getChargedCost();

        BigDecimal delta = targetChargedCost.subtract(chargedCost).setScale(2, RoundingMode.HALF_UP);
        if (delta.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Integer userId = task.getApiKey() != null && task.getApiKey().getUser() != null
                ? task.getApiKey().getUser().getUserId()
                : null;
        if (userId == null) {
            throw new BillingCalculationException("Cannot apply task debit because task user is missing: " + task.getTaskId());
        }

        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BillingCalculationException("User not found for task billing: userId=" + userId));

        BigDecimal currentBalance = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
        if (currentBalance.compareTo(delta) < 0) {
            throw new BillingCalculationException("Insufficient balance for task completion. Required=" + delta + ", available=" + currentBalance);
        }

        BigDecimal newBalance = currentBalance.subtract(delta).setScale(2, RoundingMode.HALF_UP);
        user.setBalance(newBalance);
        userRepository.save(user);

        task.setChargedCost(targetChargedCost);
        task.setRemainingBalance(newBalance);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);

        telemetryClient.trackTrace(
                "Applied task debit",
                SeverityLevel.Information,
                Map.of(
                        "taskId", String.valueOf(task.getTaskId()),
                        "userId", String.valueOf(userId),
                        "delta", delta.toPlainString(),
                        "remainingBalance", newBalance.toPlainString()
                )
        );
    }

    private BigDecimal calculateTargetChargedCost(Task task) {
        List<UsageRecord> usageRecords = usageRecordRepository.findByTask(task);
        if (!usageRecords.isEmpty()) {
            BigDecimal rawCharged = usageRecords.stream()
                    .map(this::calculateChargedAmountForRecord)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return rawCharged.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalCost = task.getTotalCost() == null ? BigDecimal.ZERO : task.getTotalCost();
        BigDecimal multiplier = resolveTaskMultiplier(task);
        return totalCost.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateChargedAmountForRecord(UsageRecord record) {
        BigDecimal cost = record.getCost() == null ? BigDecimal.ZERO : record.getCost();
        BigDecimal multiplier = resolveMultiplier(record.getBillingConfig());
        if (record.getBillingConfig() == null && record.getTask() != null) {
            multiplier = resolveTaskMultiplier(record.getTask());
        }
        return cost.multiply(multiplier);
    }

    private BigDecimal resolveTaskMultiplier(Task task) {
        Integer modelId = task.getModel() == null ? null : task.getModel().getModelId();
        if (modelId == null) {
            return BigDecimal.ONE;
        }

        BillingConfig config = billingConfigRepository.findByModelModelIdAndActiveTrue(modelId).orElse(null);
        return resolveMultiplier(config);
    }

    private BigDecimal resolveMultiplier(BillingConfig config) {
        if (config == null || config.getChargeMultiplier() == null || config.getChargeMultiplier().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return config.getChargeMultiplier();
    }
}
