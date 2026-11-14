package com.smartbackup.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record DashboardStatsResponse(
        long totalFiles,
        long totalBackups,
        long usedStorageBytes,
        String usedStorageFormatted,
        Instant lastBackupDate,
        Instant nextScheduledBackup,
        long successCount,
        long failedCount,
        double successRate,
        long activeSchedules
) {
}
