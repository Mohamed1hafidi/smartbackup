package com.smartbackup.event;

import java.util.UUID;

public record ScheduleStatusChangedEvent(
        UUID userId,
        UUID scheduleId,
        boolean active,
        boolean suspended
) {
}
