package com.fleebug.corerouter.service.activity;

import com.fleebug.corerouter.entity.activity.ActivityLog;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.activity.ActivityAction;
import com.fleebug.corerouter.repository.activity.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    @Transactional
    public void log(User user, ActivityAction action, String details, String ipAddress) {
        if (user == null || action == null) {
            return;
        }

        ActivityLog log = ActivityLog.builder()
                .user(user)
                .action(action.name())
                .details(details)
                .ipAddress(resolveIp(ipAddress))
                .createdAt(LocalDateTime.now())
                .build();

        activityLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<ActivityLog> getRecentActivityByUser(User user) {
        if (user == null) {
            return List.of();
        }
        return activityLogRepository.findTop10ByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLog> getActivityByUser(User user, int page, int size) {
        if (user == null) {
            return Page.empty();
        }

        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return activityLogRepository.findByUser(user, pageable);
    }

    private String resolveIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return "UNKNOWN";
        }
        return ipAddress.length() > 45 ? ipAddress.substring(0, 45) : ipAddress;
    }
}
