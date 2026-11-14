package com.smartbackup.dto.response;

import java.time.Instant;

public record DownloadResponse(
        String downloadUrl,
        Instant expiresAt,
        String fileName,
        long fileSize,
        String mimeType
) {
}
