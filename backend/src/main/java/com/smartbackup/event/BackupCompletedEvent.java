package com.smartbackup.event;

import com.smartbackup.entity.BackupStatus;

import java.util.UUID;

public record BackupCompletedEvent(
        UUID userId,
        UUID fileId,
        UUID backupId,
        String fileName,
        int versionNumber,
        BackupStatus status,
        String errorMessage
) {
}
