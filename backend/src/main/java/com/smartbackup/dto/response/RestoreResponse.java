package com.smartbackup.dto.response;

import com.smartbackup.entity.RestoreJobEntity;
import com.smartbackup.entity.RestoreStatus;

import java.time.Instant;
import java.util.UUID;

public record RestoreResponse(
        UUID restoreJobId,
        UUID backupId,
        UUID fileId,
        RestoreStatus status,
        Instant initiatedAt,
        Instant completedAt,
        String errorMessage
) {

    public static RestoreResponse from(RestoreJobEntity job) {
        return new RestoreResponse(
                job.getId(),
                job.getBackupId(),
                job.getFileId(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getCompletedAt(),
                job.getErrorMessage()
        );
    }
}
