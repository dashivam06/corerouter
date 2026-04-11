package com.fleebug.corerouter.service.billing;

import com.fleebug.corerouter.entity.task.Task;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.task.TaskStatus;
import com.fleebug.corerouter.exception.billing.BillingCalculationException;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskBillingService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final TelemetryClient telemetryClient;

    public void applyDebitIfEligible(Task task) {
        if (task == null || task.getStatus() != TaskStatus.COMPLETED) {
            return;
        }

        BigDecimal totalCost = task.getTotalCost() == null ? BigDecimal.ZERO : task.getTotalCost();
        BigDecimal targetChargedCost = totalCost.setScale(2, RoundingMode.HALF_UP);
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
}
