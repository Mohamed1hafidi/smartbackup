package com.smartbackup.scheduler;

import com.smartbackup.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class BackupSchedulerJob {

    private final ScheduleService scheduleService;

    @Scheduled(
            fixedDelayString = "${app.scheduler.interval-ms:60000}",
            initialDelayString = "${app.scheduler.initial-delay-ms:30000}"
    )
    public void executeDueSchedules() {
        int executed = scheduleService.executeDueSchedules();
        if (executed > 0) {
            log.info("Scheduler : {} sauvegarde(s) planifiée(s) exécutée(s)", executed);
        }
    }
}
