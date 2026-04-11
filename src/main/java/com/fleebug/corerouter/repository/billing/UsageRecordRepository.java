package com.fleebug.corerouter.repository.billing;

import com.fleebug.corerouter.entity.billing.UsageRecord;
import com.fleebug.corerouter.entity.task.Task;
import com.fleebug.corerouter.enums.apikey.ApiKeyStatus;
import com.fleebug.corerouter.enums.billing.UsageUnitType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {

    List<UsageRecord> findByTask(Task task);

    List<UsageRecord> findByTaskTaskId(String taskId);

    // Total cost for an API key in a date range
    @Query("SELECT COALESCE(SUM(u.cost), 0) FROM UsageRecord u " +
           "WHERE u.apiKey.apiKeyId = :apiKeyId " +
           "AND u.recordedAt BETWEEN :from AND :to")
    BigDecimal sumCostByApiKeyAndPeriod(
        @Param("apiKeyId") Integer apiKeyId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    // Usage breakdown by unit type [UsageUnitType, totalQuantity, totalCost]
    @Query("SELECT u.usageUnitType, COALESCE(SUM(u.quantity), 0), COALESCE(SUM(u.cost), 0) " +
           "FROM UsageRecord u " +
           "WHERE u.apiKey.apiKeyId = :apiKeyId " +
           "AND u.recordedAt BETWEEN :from AND :to " +
           "GROUP BY u.usageUnitType")
    List<Object[]> sumUsageByApiKeyGroupedByUnitType(
        @Param("apiKeyId") Integer apiKeyId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    // Usage breakdown by model + unit type [modelId, usageUnitType, totalQty, totalCost, count]
    @Query("SELECT u.model.modelId, u.usageUnitType, " +
           "COALESCE(SUM(u.quantity), 0), COALESCE(SUM(u.cost), 0), COUNT(u) " +
           "FROM UsageRecord u " +
           "WHERE u.apiKey.apiKeyId = :apiKeyId " +
           "AND u.recordedAt BETWEEN :from AND :to " +
           "GROUP BY u.model.modelId, u.usageUnitType")
    List<Object[]> sumUsageByApiKeyGroupedByModelAndUnitType(
        @Param("apiKeyId") Integer apiKeyId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    Page<UsageRecord> findByApiKeyApiKeyIdAndRecordedAtBetween(
        Integer apiKeyId,
        LocalDateTime from,
        LocalDateTime to,
        Pageable pageable
    );

    long countByApiKeyApiKeyIdAndRecordedAtBetween(
        Integer apiKeyId,
        LocalDateTime from,
        LocalDateTime to
    );

    // Total usage for a model + unit type
    @Query("SELECT COALESCE(SUM(u.quantity), 0), COALESCE(SUM(u.cost), 0) " +
           "FROM UsageRecord u " +
           "WHERE u.model.modelId = :modelId " +
           "AND u.usageUnitType = :unitType " +
           "AND u.recordedAt BETWEEN :from AND :to")
    List<Object[]> sumUsageByModelAndUnitType(
        @Param("modelId") Integer modelId,
        @Param("unitType") UsageUnitType unitType,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    // Total cost for a user across all their API keys
    @Query("SELECT COALESCE(SUM(u.cost), 0) FROM UsageRecord u " +
           "WHERE u.apiKey.user.userId = :userId " +
           "AND u.recordedAt BETWEEN :from AND :to")
    BigDecimal sumCostByUserAndPeriod(
        @Param("userId") Integer userId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    @Query("SELECT COALESCE(SUM(u.cost), 0) FROM UsageRecord u " +
           "WHERE u.recordedAt BETWEEN :from AND :to")
    BigDecimal sumCostByPeriod(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    @Query("SELECT COUNT(DISTINCT u.task.taskId) FROM UsageRecord u " +
           "WHERE u.apiKey.user.userId = :userId " +
           "AND u.recordedAt BETWEEN :from AND :to")
    long countDistinctRequestsByUserAndPeriod(
        @Param("userId") Integer userId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    @Query("SELECT u.model.fullname, COUNT(DISTINCT u.task.taskId) " +
           "FROM UsageRecord u " +
           "WHERE u.apiKey.user.userId = :userId " +
           "AND u.recordedAt BETWEEN :from AND :to " +
           "GROUP BY u.model.fullname " +
           "ORDER BY COUNT(DISTINCT u.task.taskId) DESC")
    List<Object[]> findTopModelsByUserAndPeriod(
        @Param("userId") Integer userId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable
    );

    @Query("SELECT u.model.type, COUNT(DISTINCT u.task.taskId) " +
           "FROM UsageRecord u " +
           "WHERE u.apiKey.user.userId = :userId " +
           "AND u.recordedAt BETWEEN :from AND :to " +
           "GROUP BY u.model.type")
    List<Object[]> countDistinctRequestsByUserGroupedByModelTypeAndPeriod(
        @Param("userId") Integer userId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    @Query("SELECT u.model.type, COUNT(DISTINCT u.task.taskId) " +
           "FROM UsageRecord u " +
           "WHERE u.apiKey.user.userId = :userId " +
           "AND u.apiKey.status = :status " +
           "GROUP BY u.model.type")
    List<Object[]> countDistinctRequestsByUserGroupedByModelTypeAndActiveApiKeyStatus(
        @Param("userId") Integer userId,
        @Param("status") ApiKeyStatus status
    );

    @Query("SELECT FUNCTION('DATE', u.recordedAt), COALESCE(SUM(u.cost), 0) " +
           "FROM UsageRecord u " +
           "WHERE u.apiKey.user.userId = :userId " +
           "AND u.recordedAt BETWEEN :from AND :to " +
           "GROUP BY FUNCTION('DATE', u.recordedAt) " +
           "ORDER BY FUNCTION('DATE', u.recordedAt)")
    List<Object[]> sumCostByUserGroupedByDateAndPeriod(
        @Param("userId") Integer userId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    @Query("SELECT FUNCTION('DATE', u.recordedAt), u.usageUnitType, COALESCE(SUM(u.quantity), 0), COALESCE(SUM(u.cost), 0) " +
           "FROM UsageRecord u " +
           "WHERE u.apiKey.user.userId = :userId " +
           "AND u.recordedAt BETWEEN :from AND :to " +
           "GROUP BY FUNCTION('DATE', u.recordedAt), u.usageUnitType " +
           "ORDER BY FUNCTION('DATE', u.recordedAt)")
    List<Object[]> sumUsageByUserGroupedByDateAndUnitTypeAndPeriod(
        @Param("userId") Integer userId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    @Query("SELECT FUNCTION('DATE', u.recordedAt), COUNT(DISTINCT u.task.taskId) " +
           "FROM UsageRecord u " +
           "WHERE u.apiKey.user.userId = :userId " +
           "AND u.recordedAt BETWEEN :from AND :to " +
           "GROUP BY FUNCTION('DATE', u.recordedAt) " +
           "ORDER BY FUNCTION('DATE', u.recordedAt)")
    List<Object[]> countDistinctRequestsByUserGroupedByDateAndPeriod(
        @Param("userId") Integer userId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );
}
