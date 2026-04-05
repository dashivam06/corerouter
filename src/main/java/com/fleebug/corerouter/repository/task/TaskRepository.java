package com.fleebug.corerouter.repository.task;

import com.fleebug.corerouter.entity.task.Task;
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

    long countByApiKey_User_UserId(Integer userId);

    long countByApiKey_User_UserIdAndStatus(Integer userId, TaskStatus status);

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

        Page<Task> findByStatus(TaskStatus status, Pageable pageable);
}

