package com.smartbackup.dto.response;

import com.smartbackup.entity.FileEntity;

import java.time.Instant;
import java.util.UUID;

public record FileSummaryResponse(
        UUID id,
        String originalName,
        long fileSize,
        String mimeType,
        Instant createdAt,
        Instant lastBackupAt
) {

    public static FileSummaryResponse from(FileEntity file) {
        return new FileSummaryResponse(
                file.getId(),
                file.getOriginalName(),
                file.getFileSize(),
                file.getMimeType(),
                file.getCreatedAt(),
                null
        );
    }
}
