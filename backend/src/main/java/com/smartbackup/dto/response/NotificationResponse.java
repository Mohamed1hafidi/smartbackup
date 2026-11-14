package com.smartbackup.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartbackup.entity.NotificationEntity;
import com.smartbackup.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record NotificationResponse(
        UUID id,
        UUID backupId,
        NotificationType type,
        String message,
        boolean isRead,
        Instant createdAt
) {

    public static NotificationResponse from(NotificationEntity notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getBackupId(),
                notification.getType(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
