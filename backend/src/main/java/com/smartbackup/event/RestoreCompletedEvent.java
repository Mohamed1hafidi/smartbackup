package com.smartbackup.event;

import com.smartbackup.entity.RestoreStatus;

import java.util.UUID;

public record RestoreCompletedEvent(
        UUID userId,
        UUID fileId,
        UUID backupId,
        String fileName,
        RestoreStatus status,
        String errorMessage
) {
}
