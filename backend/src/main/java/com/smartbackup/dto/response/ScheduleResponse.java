package com.smartbackup.dto.response;

import com.smartbackup.entity.BackupScheduleEntity;
import com.smartbackup.entity.ScheduleFrequency;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record ScheduleResponse(
        UUID id,
        UUID fileId,
        String fileName,
        ScheduleFrequency frequency,
        String scheduledTime,
        DayOfWeek dayOfWeek,
        String timezone,
        boolean isActive,
        Instant lastRunAt,
        Instant nextRunAt,
        Instant createdAt,
        Instant updatedAt
) {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public static ScheduleResponse from(BackupScheduleEntity schedule, String fileName) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getFileId(),
                fileName,
                schedule.getFrequency(),
                schedule.getScheduledTime().format(TIME_FORMAT),
                schedule.getDayOfWeek(),
                schedule.getTimezone(),
                schedule.isActive(),
                schedule.getLastRunAt(),
                schedule.getNextRunAt(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
