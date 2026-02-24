package com.fleebug.corerouter.repository.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fleebug.corerouter.entity.activity.ActivityLog;
import com.fleebug.corerouter.entity.user.User;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Integer> {

    List<ActivityLog> findByUser(User user);

    List<ActivityLog> findByUserAndCreatedAtBetween(User user, LocalDateTime start, LocalDateTime end);

    List<ActivityLog> findByDetailsContainingIgnoreCase(String keyword);
}
