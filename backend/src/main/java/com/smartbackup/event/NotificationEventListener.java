package com.smartbackup.event;

import com.smartbackup.entity.BackupStatus;
import com.smartbackup.entity.NotificationType;
import com.smartbackup.entity.RestoreStatus;
import com.smartbackup.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private static final int MAX_ERROR_LENGTH = 200;

    private final NotificationService notificationService;

    @EventListener
    public void onBackupCompleted(BackupCompletedEvent event) {
        try {
            if (event.status() == BackupStatus.SUCCESS) {
                notificationService.create(
                        event.userId(),
                        event.backupId(),
                        NotificationType.BACKUP_SUCCESS,
                        backupSuccessMessage(event));
            } else if (event.status() == BackupStatus.FAILED) {
                notificationService.create(
                        event.userId(),
                        event.backupId(),
                        NotificationType.BACKUP_FAILED,
                        backupFailureMessage(event));
            }
        } catch (RuntimeException exception) {
            log.error("Impossible de créer la notification de sauvegarde {}", event.backupId(),
                    exception);
        }
    }

    @EventListener
    public void onRestoreCompleted(RestoreCompletedEvent event) {
        try {
            if (event.status() == RestoreStatus.SUCCESS) {
                notificationService.create(
                        event.userId(),
                        event.backupId(),
                        NotificationType.RESTORE_SUCCESS,
                        restoreMessage(event, true));
            } else if (event.status() == RestoreStatus.FAILED) {
                notificationService.create(
                        event.userId(),
                        event.backupId(),
                        NotificationType.RESTORE_FAILED,
                        restoreMessage(event, false));
            }
        } catch (RuntimeException exception) {
            log.error("Impossible de créer la notification de restauration {}", event.backupId(),
                    exception);
        }
    }

    @EventListener
    public void onScheduleStatusChanged(ScheduleStatusChangedEvent event) {
        try {
            if (event.active()) {
                notificationService.create(
                        event.userId(),
                        null,
                        NotificationType.SCHEDULE_ACTIVATED,
                        "Programmation activée");
            } else {
                String message = event.suspended()
                        ? "Programmation suspendue après 3 échecs consécutifs"
                        : "Programmation désactivée";
                notificationService.create(
                        event.userId(),
                        null,
                        NotificationType.SCHEDULE_DEACTIVATED,
                        message);
            }
        } catch (RuntimeException exception) {
            log.error("Impossible de créer la notification de programmation {}",
                    event.scheduleId(), exception);
        }
    }

    private String backupSuccessMessage(BackupCompletedEvent event) {
        if (event.fileName() == null || event.fileName().isBlank()) {
            return "Sauvegarde réussie (version " + event.versionNumber() + ")";
        }
        return "Sauvegarde du fichier '%s' réussie (version %d)"
                .formatted(event.fileName(), event.versionNumber());
    }

    private String backupFailureMessage(BackupCompletedEvent event) {
        String prefix = event.fileName() == null || event.fileName().isBlank()
                ? "Sauvegarde échouée"
                : "Sauvegarde du fichier '%s' échouée".formatted(event.fileName());
        return appendError(prefix, event.errorMessage());
    }

    private String restoreMessage(RestoreCompletedEvent event, boolean success) {
        String prefix;
        if (event.fileName() == null || event.fileName().isBlank()) {
            prefix = success ? "Restauration réussie" : "Restauration échouée";
        } else {
            prefix = success
                    ? "Restauration du fichier '%s' réussie".formatted(event.fileName())
                    : "Restauration du fichier '%s' échouée".formatted(event.fileName());
        }
        return success ? prefix : appendError(prefix, event.errorMessage());
    }

    private String appendError(String message, String error) {
        if (error == null || error.isBlank()) {
            return message;
        }
        String truncated = error.length() <= MAX_ERROR_LENGTH
                ? error
                : error.substring(0, MAX_ERROR_LENGTH);
        return message + " : " + truncated;
    }
}
