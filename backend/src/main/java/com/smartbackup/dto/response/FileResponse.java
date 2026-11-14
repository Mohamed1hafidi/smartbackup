package com.smartbackup.dto.response;

import com.smartbackup.entity.FileEntity;

import java.time.Instant;
import java.util.UUID;

public record FileResponse(
        UUID id,
        String originalName,
        String storedKey,
        long fileSize,
        String mimeType,
        String sha256Hash,
        Instant createdAt,
        Instant updatedAt,
        long backupCount,
        Instant lastBackupAt
) {

    public static FileResponse from(FileEntity file) {
        return new FileResponse(
                file.getId(),
                file.getOriginalName(),
                file.getStoredKey(),
                file.getFileSize(),
                file.getMimeType(),
                file.getSha256Hash(),
                file.getCreatedAt(),
                file.getUpdatedAt(),
                0,
                null
        );
    }
}
