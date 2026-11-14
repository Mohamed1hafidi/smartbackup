package com.smartbackup.dto.response;

import com.smartbackup.entity.BackupEntity;
import com.smartbackup.entity.BackupStatus;
import com.smartbackup.entity.BackupType;

import java.time.Instant;
import java.util.UUID;

public record BackupSummaryResponse(
        UUID id,
        UUID fileId,
        String fileName,
        BackupType backupType,
        BackupStatus status,
        long fileSize,
        int versionNumber,
        Instant createdAt,
        Instant completedAt
) {

    public static BackupSummaryResponse from(BackupEntity backup, String fileName) {
        return new BackupSummaryResponse(
                backup.getId(),
                backup.getFileId(),
                fileName,
                backup.getBackupType(),
                backup.getStatus(),
                backup.getFileSize(),
                backup.getVersionNumber(),
                backup.getCreatedAt(),
                backup.getCompletedAt()
        );
    }
}
