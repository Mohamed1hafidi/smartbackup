package com.smartbackup.service;

import com.smartbackup.dto.response.DashboardStatsResponse;
import com.smartbackup.entity.BackupEntity;
import com.smartbackup.entity.BackupScheduleEntity;
import com.smartbackup.entity.BackupStatus;
import com.smartbackup.repository.BackupRepository;
import com.smartbackup.repository.BackupScheduleRepository;
import com.smartbackup.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final String[] STORAGE_UNITS = {"KB", "MB", "GB", "TB", "PB"};

    private final FileRepository fileRepository;
    private final BackupRepository backupRepository;
    private final BackupScheduleRepository scheduleRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats(UUID userId) {
        long totalFiles = fileRepository.countByUserIdAndDeletedFalse(userId);
        long totalBackups = backupRepository.countByUserId(userId);
        long usedStorageBytes = fileRepository.sumActiveFileSizeByUserId(userId);
        long successCount = backupRepository.countByUserIdAndStatus(userId, BackupStatus.SUCCESS);
        long failedCount = backupRepository.countByUserIdAndStatus(userId, BackupStatus.FAILED);
        long activeSchedules = scheduleRepository.countByUserIdAndActiveTrue(userId);

        Instant lastBackupDate = backupRepository
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(BackupEntity::getCreatedAt)
                .orElse(null);

        Instant nextScheduledBackup = scheduleRepository
                .findFirstByUserIdAndActiveTrueAndNextRunAtIsNotNullOrderByNextRunAtAsc(userId)
                .map(BackupScheduleEntity::getNextRunAt)
                .orElse(null);

        return new DashboardStatsResponse(
                totalFiles,
                totalBackups,
                usedStorageBytes,
                formatBytes(usedStorageBytes),
                lastBackupDate,
                nextScheduledBackup,
                successCount,
                failedCount,
                successRate(successCount, failedCount),
                activeSchedules
        );
    }

    private double successRate(long successCount, long failedCount) {
        long finished = successCount + failedCount;
        if (finished == 0) {
            return 0.0;
        }
        double rate = successCount * 100.0 / finished;
        return Math.round(rate * 10.0) / 10.0;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        double value = bytes;
        int unitIndex = -1;
        do {
            value /= 1024.0;
            unitIndex++;
        } while (value >= 1024.0 && unitIndex < STORAGE_UNITS.length - 1);

        return String.format(Locale.ROOT, "%.1f %s", value, STORAGE_UNITS[unitIndex]);
    }
}
