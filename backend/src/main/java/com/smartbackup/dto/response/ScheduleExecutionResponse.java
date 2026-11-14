package com.smartbackup.dto.response;

import com.smartbackup.entity.ScheduleExecutionEntity;
import com.smartbackup.entity.ScheduleExecutionStatus;

import java.time.Instant;
import java.util.UUID;

public record ScheduleExecutionResponse(
        UUID id,
        UUID scheduleId,
        UUID backupId,
        ScheduleExecutionStatus status,
        String errorMessage,
        Long durationMs,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt
) {

    public static ScheduleExecutionResponse from(ScheduleExecutionEntity execution) {
        return new ScheduleExecutionResponse(
                execution.getId(),
                execution.getScheduleId(),
                execution.getBackupId(),
                execution.getStatus(),
                execution.getErrorMessage(),
                execution.getDurationMs(),
                execution.getStartedAt(),
                execution.getCompletedAt(),
                execution.getCreatedAt()
        );
    }
}
