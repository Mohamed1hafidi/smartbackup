package com.smartbackup.dto.response;

import com.smartbackup.entity.BackupEntity;
import com.smartbackup.entity.BackupStatus;
import com.smartbackup.entity.BackupType;

import java.time.Instant;
import java.util.UUID;

public record BackupResponse(
        UUID id,
        UUID fileId,
        String fileName,
        String r2ObjectKey,
        String r2Bucket,
        long fileSize,
        String sha256Hash,
        BackupType backupType,
        BackupStatus status,
        String errorMessage,
        int versionNumber,
        Instant createdAt,
        Instant completedAt
) {

    public static BackupResponse from(BackupEntity backup, String fileName) {
        return new BackupResponse(
                backup.getId(),
                backup.getFileId(),
                fileName,
                backup.getR2ObjectKey(),
                backup.getR2Bucket(),
                backup.getFileSize(),
                backup.getSha256Hash(),
                backup.getBackupType(),
                backup.getStatus(),
                backup.getErrorMessage(),
                backup.getVersionNumber(),
                backup.getCreatedAt(),
                backup.getCompletedAt()
        );
    }
}
