package com.smartbackup.dto.request;

import com.smartbackup.entity.ScheduleFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.DayOfWeek;

public record UpdateScheduleRequest(
        @NotNull(message = "frequency est requise")
        ScheduleFrequency frequency,

        @NotBlank(message = "scheduledTime est requis")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
                message = "scheduledTime doit être au format HH:mm")
        String scheduledTime,

        DayOfWeek dayOfWeek,

        @NotBlank(message = "timezone est requis")
        String timezone
) {
}
