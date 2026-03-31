package com.fleebug.corerouter.service.activity;

import com.fleebug.corerouter.entity.activity.ActivityLog;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.activity.ActivityAction;
import com.fleebug.corerouter.repository.activity.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    private String resolveIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return "UNKNOWN";
        }
        return ipAddress.length() > 45 ? ipAddress.substring(0, 45) : ipAddress;
    }
}
