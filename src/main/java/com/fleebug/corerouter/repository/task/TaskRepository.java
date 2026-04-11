package com.fleebug.corerouter.repository.task;

import com.fleebug.corerouter.entity.task.Task;
import com.fleebug.corerouter.enums.apikey.ApiKeyStatus;
import com.fleebug.corerouter.enums.task.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, String> {

    Optional<Task> findByTaskId(String taskId);

    long countByStatus(TaskStatus status);

    long countByStatusAndCompletedAtBetween(TaskStatus status, LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(COALESCE(t.totalCost, 0)), 0) FROM Task t WHERE t.status = :status")
    java.math.BigDecimal sumTotalCostByStatus(@Param("status") TaskStatus status);

        @Query("SELECT COALESCE(SUM(COALESCE(t.chargedCost, t.totalCost, 0)), 0) FROM Task t WHERE t.status = :status")
        java.math.BigDecimal sumChargedCostByStatus(@Param("status") TaskStatus status);

    @Query("SELECT COALESCE(SUM(COALESCE(t.totalCost, 0)), 0) FROM Task t WHERE t.status = :status AND t.completedAt BETWEEN :from AND :to")
    java.math.BigDecimal sumTotalCostByStatusAndCompletedAtBetween(@Param("status") TaskStatus status,
                                                                   @Param("from") LocalDateTime from,
                                                                   @Param("to") LocalDateTime to);

        @Query("SELECT COALESCE(SUM(COALESCE(t.chargedCost, t.totalCost, 0)), 0) FROM Task t WHERE t.status = :status AND t.completedAt BETWEEN :from AND :to")
        java.math.BigDecimal sumChargedCostByStatusAndCompletedAtBetween(@Param("status") TaskStatus status,
                                                                                                                                          @Param("from") LocalDateTime from,
                                                                                                                                          @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(DISTINCT t.apiKey.user.userId) FROM Task t WHERE t.createdAt BETWEEN :from AND :to")
    long countDistinctUsersByCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT FUNCTION('HOUR', t.createdAt), COUNT(t) FROM Task t " +
            "WHERE t.createdAt BETWEEN :from AND :to " +
            "GROUP BY FUNCTION('HOUR', t.createdAt)")
    List<Object[]> countByHourBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

        @Query("SELECT FUNCTION('HOUR', t.completedAt), COALESCE(SUM(COALESCE(t.totalCost, 0)), 0) FROM Task t " +
            "WHERE t.completedAt BETWEEN :from AND :to " +
            "AND t.status = :status " +
            "GROUP BY FUNCTION('HOUR', t.completedAt)")
        List<Object[]> sumTotalCostByHourBetween(@Param("from") LocalDateTime from,
                                                                                         @Param("to") LocalDateTime to,
                                                                                         @Param("status") TaskStatus status);

    @Query("SELECT FUNCTION('HOUR', t.completedAt), COALESCE(SUM(COALESCE(t.chargedCost, t.totalCost, 0)), 0) FROM Task t " +
            "WHERE t.completedAt BETWEEN :from AND :to " +
            "AND t.status = :status " +
            "GROUP BY FUNCTION('HOUR', t.completedAt)")
    List<Object[]> sumChargedCostByHourBetween(@Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to,
                                                @Param("status") TaskStatus status);

    List<Task> findTop10ByStatusAndCompletedAtIsNotNullOrderByCompletedAtDesc(TaskStatus status);

    List<Task> findByCreatedAtBetweenOrderByCreatedAtAsc(LocalDateTime from, LocalDateTime to);

    List<Task> findByApiKey_User_UserIdAndStatusAndCompletedAtBetweenOrderByCompletedAtDesc(
            Integer userId,
            TaskStatus status,
            LocalDateTime from,
            LocalDateTime to);

    List<Task> findByApiKey_User_UserIdAndStatusAndCompletedAtBetweenAndRemainingBalanceIsNotNullOrderByCompletedAtDesc(
                Integer userId,
                TaskStatus status,
                LocalDateTime from,
                LocalDateTime to);

    long countByApiKey_User_UserId(Integer userId);

    long countByApiKey_User_UserIdAndCreatedAtBetween(Integer userId, LocalDateTime from, LocalDateTime to);

    long countByApiKey_User_UserIdAndStatus(Integer userId, TaskStatus status);

    long countByApiKey_User_UserIdAndStatusAndCompletedAtBetween(Integer userId, TaskStatus status, LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(COALESCE(t.totalCost, 0)), 0) FROM Task t " +
            "WHERE t.apiKey.user.userId = :userId " +
            "AND t.status = :status " +
            "AND t.completedAt BETWEEN :from AND :to")
    java.math.BigDecimal sumTotalCostByUserAndStatusAndCompletedAtBetween(
            @Param("userId") Integer userId,
            @Param("status") TaskStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(COALESCE(t.chargedCost, t.totalCost, 0)), 0) FROM Task t " +
            "WHERE t.apiKey.user.userId = :userId " +
            "AND t.status = :status " +
            "AND t.completedAt BETWEEN :from AND :to")
    java.math.BigDecimal sumChargedCostByUserAndStatusAndCompletedAtBetween(
            @Param("userId") Integer userId,
            @Param("status") TaskStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT FUNCTION('DATE', t.completedAt), COALESCE(SUM(COALESCE(t.totalCost, 0)), 0) " +
            "FROM Task t " +
            "WHERE t.apiKey.user.userId = :userId " +
            "AND t.status = :status " +
            "AND t.completedAt BETWEEN :from AND :to " +
            "GROUP BY FUNCTION('DATE', t.completedAt) " +
            "ORDER BY FUNCTION('DATE', t.completedAt)")
    List<Object[]> sumTotalCostByUserAndStatusGroupedByDateBetween(
            @Param("userId") Integer userId,
            @Param("status") TaskStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT FUNCTION('DATE', t.completedAt), COALESCE(SUM(COALESCE(t.chargedCost, t.totalCost, 0)), 0) " +
            "FROM Task t " +
            "WHERE t.apiKey.user.userId = :userId " +
            "AND t.status = :status " +
            "AND t.completedAt BETWEEN :from AND :to " +
            "GROUP BY FUNCTION('DATE', t.completedAt) " +
            "ORDER BY FUNCTION('DATE', t.completedAt)")
    List<Object[]> sumChargedCostByUserAndStatusGroupedByDateBetween(
            @Param("userId") Integer userId,
            @Param("status") TaskStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT FUNCTION('DATE', t.completedAt), COUNT(t) " +
            "FROM Task t " +
            "WHERE t.apiKey.user.userId = :userId " +
            "AND t.status = :status " +
            "AND t.completedAt BETWEEN :from AND :to " +
            "GROUP BY FUNCTION('DATE', t.completedAt) " +
            "ORDER BY FUNCTION('DATE', t.completedAt)")
    List<Object[]> countByUserAndStatusGroupedByCompletedDateBetween(
            @Param("userId") Integer userId,
            @Param("status") TaskStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT FUNCTION('DATE', t.createdAt), t.status, COUNT(t) " +
            "FROM Task t " +
            "WHERE t.createdAt BETWEEN :from AND :to " +
            "AND (:status IS NULL OR t.status = :status) " +
            "GROUP BY FUNCTION('DATE', t.createdAt), t.status " +
            "ORDER BY FUNCTION('DATE', t.createdAt)")
    List<Object[]> countPerDayAndStatusBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") TaskStatus status);

    @Query("SELECT t.model.type, COUNT(t) " +
            "FROM Task t " +
            "WHERE t.apiKey.user.userId = :userId " +
            "AND t.status = :status " +
            "AND t.createdAt BETWEEN :from AND :to " +
            "GROUP BY t.model.type")
    List<Object[]> countByUserGroupedByModelTypeAndCreatedAtBetween(
            @Param("userId") Integer userId,
            @Param("status") TaskStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT t.model.type, COUNT(t) " +
            "FROM Task t " +
            "WHERE t.apiKey.user.userId = :userId " +
            "AND t.apiKey.status = :apiKeyStatus " +
            "AND t.status = :status " +
            "GROUP BY t.model.type")
    List<Object[]> countByUserGroupedByModelTypeAndActiveApiKeyStatus(
            @Param("userId") Integer userId,
            @Param("apiKeyStatus") ApiKeyStatus apiKeyStatus,
            @Param("status") TaskStatus status);

    @Query("SELECT t.model.fullname, COUNT(t) " +
            "FROM Task t " +
            "WHERE t.apiKey.user.userId = :userId " +
            "AND t.status = :status " +
            "AND t.completedAt BETWEEN :from AND :to " +
            "GROUP BY t.model.fullname " +
            "ORDER BY COUNT(t) DESC")
    List<Object[]> findTopModelsByUserAndStatusAndCompletedAtBetween(
            @Param("userId") Integer userId,
            @Param("status") TaskStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);
}

