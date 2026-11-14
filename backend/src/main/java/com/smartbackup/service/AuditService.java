package com.smartbackup.service;

import com.smartbackup.entity.ActivityLogEntity;
import com.smartbackup.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final ActivityLogRepository activityLogRepository;

    public void log(
            UUID userId,
            String action,
            String resourceType,
            UUID resourceId,
            Map<String, Object> details,
            String ipAddress
    ) {
        ActivityLogEntity entry = ActivityLogEntity.builder()
                .userId(userId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .details(details)
                .ipAddress(ipAddress)
                .build();
        activityLogRepository.save(entry);
    }
}
