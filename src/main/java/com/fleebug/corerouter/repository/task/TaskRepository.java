package com.fleebug.corerouter.repository.task;

import com.fleebug.corerouter.entity.task.Task;
import com.fleebug.corerouter.enums.task.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, String> {

    Optional<Task> findByTaskId(String taskId);

    long countByStatus(TaskStatus status);

    long countByApiKey_User_UserId(Integer userId);

    long countByApiKey_User_UserIdAndStatus(Integer userId, TaskStatus status);
}

