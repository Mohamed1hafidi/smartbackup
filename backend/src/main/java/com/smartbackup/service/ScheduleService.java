package com.smartbackup.service;

import com.smartbackup.dto.request.CreateScheduleRequest;
import com.smartbackup.dto.request.UpdateScheduleRequest;
import com.smartbackup.dto.response.PageResponse;
import com.smartbackup.dto.response.ScheduleExecutionResponse;
import com.smartbackup.dto.response.ScheduleResponse;
import com.smartbackup.entity.BackupScheduleEntity;
import com.smartbackup.entity.BackupStatus;
import com.smartbackup.entity.BackupType;
import com.smartbackup.entity.FileEntity;
import com.smartbackup.entity.ScheduleExecutionEntity;
import com.smartbackup.entity.ScheduleExecutionStatus;
import com.smartbackup.entity.ScheduleFrequency;
import com.smartbackup.event.ScheduleStatusChangedEvent;
import com.smartbackup.exception.BadRequestException;
import com.smartbackup.exception.QuotaExceededException;
import com.smartbackup.exception.ResourceNotFoundException;
import com.smartbackup.exception.ScheduleConflictException;
import com.smartbackup.repository.BackupScheduleRepository;
import com.smartbackup.repository.FileRepository;
import com.smartbackup.repository.ScheduleExecutionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ScheduleService {

    private static final int MAX_ACTIVE_SCHEDULES = 5;
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    private final BackupScheduleRepository scheduleRepository;
    private final ScheduleExecutionRepository executionRepository;
    private final FileRepository fileRepository;
    private final BackupService backupService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public ScheduleService(
            BackupScheduleRepository scheduleRepository,
            ScheduleExecutionRepository executionRepository,
            FileRepository fileRepository,
            BackupService backupService,
            AuditService auditService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.scheduleRepository = scheduleRepository;
        this.executionRepository = executionRepository;
        this.fileRepository = fileRepository;
        this.backupService = backupService;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    public ScheduleResponse create(
            UUID userId, CreateScheduleRequest request, String ipAddress) {
        FileEntity file = getOwnedFile(userId, request.fileId());
        ValidatedSchedule validated = validate(
                request.frequency(), request.scheduledTime(), request.dayOfWeek(),
                request.timezone());

        if (scheduleRepository.existsByUserIdAndFileIdAndActiveTrue(userId, file.getId())) {
            throw new ScheduleConflictException(
                    "Une programmation active existe déjà pour ce fichier");
        }
        if (scheduleRepository.countByUserIdAndActiveTrue(userId) >= MAX_ACTIVE_SCHEDULES) {
            throw new QuotaExceededException(
                    "Limite de 5 programmations actives atteinte");
        }

        BackupScheduleEntity schedule = BackupScheduleEntity.builder()
                .userId(userId)
                .fileId(file.getId())
                .frequency(validated.frequency())
                .scheduledTime(validated.scheduledTime())
                .dayOfWeek(validated.dayOfWeek())
                .timezone(validated.timezone().getId())
                .active(true)
                .consecutiveFailures(0)
                .build();
        schedule.setNextRunAt(calculateNextRunAt(schedule, Instant.now()));
        schedule = scheduleRepository.save(schedule);

        safeAudit(userId, "SCHEDULE_CREATED", "SCHEDULE", schedule.getId(),
                scheduleDetails(schedule), ipAddress);
        eventPublisher.publishEvent(new ScheduleStatusChangedEvent(
                userId, schedule.getId(), true, false));

        log.info("Programmation {} créée pour le fichier {} ({} à {})",
                schedule.getId(), file.getId(), schedule.getFrequency(),
                schedule.getScheduledTime());
        return ScheduleResponse.from(schedule, file.getOriginalName());
    }

    public List<ScheduleResponse> list(UUID userId) {
        List<BackupScheduleEntity> schedules =
                scheduleRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<UUID, String> fileNames = loadFileNames(schedules);

        return schedules.stream()
                .map(schedule -> ScheduleResponse.from(
                        schedule, fileNames.get(schedule.getFileId())))
                .toList();
    }

    public ScheduleResponse get(UUID userId, UUID scheduleId) {
        BackupScheduleEntity schedule = getOwnedSchedule(userId, scheduleId);
        return toResponse(schedule);
    }

    public ScheduleResponse update(
            UUID userId,
            UUID scheduleId,
            UpdateScheduleRequest request,
            String ipAddress
    ) {
        BackupScheduleEntity schedule = getOwnedSchedule(userId, scheduleId);
        ValidatedSchedule validated = validate(
                request.frequency(), request.scheduledTime(), request.dayOfWeek(),
                request.timezone());

        if (schedule.isActive() && scheduleRepository
                .existsByUserIdAndFileIdAndActiveTrueAndIdNot(
                        userId, schedule.getFileId(), schedule.getId())) {
            throw new ScheduleConflictException(
                    "Une programmation active existe déjà pour ce fichier");
        }

        schedule.setFrequency(validated.frequency());
        schedule.setScheduledTime(validated.scheduledTime());
        schedule.setDayOfWeek(validated.dayOfWeek());
        schedule.setTimezone(validated.timezone().getId());
        schedule.setNextRunAt(schedule.isActive()
                ? calculateNextRunAt(schedule, Instant.now())
                : null);
        schedule = scheduleRepository.save(schedule);

        safeAudit(userId, "SCHEDULE_UPDATED", "SCHEDULE", schedule.getId(),
                scheduleDetails(schedule), ipAddress);

        log.info("Programmation {} mise à jour", schedule.getId());
        return toResponse(schedule);
    }

    public void delete(UUID userId, UUID scheduleId, String ipAddress) {
        BackupScheduleEntity schedule = getOwnedSchedule(userId, scheduleId);
        Map<String, Object> details = Map.of(
                "fileId", schedule.getFileId().toString(),
                "frequency", schedule.getFrequency().name());

        scheduleRepository.delete(schedule);
        safeAudit(userId, "SCHEDULE_DELETED", "SCHEDULE", scheduleId,
                details, ipAddress);

        log.info("Programmation {} supprimée", scheduleId);
    }

    public ScheduleResponse toggle(UUID userId, UUID scheduleId, String ipAddress) {
        BackupScheduleEntity schedule = getOwnedSchedule(userId, scheduleId);

        if (!schedule.isActive()) {
            if (scheduleRepository.existsByUserIdAndFileIdAndActiveTrueAndIdNot(
                    userId, schedule.getFileId(), schedule.getId())) {
                throw new ScheduleConflictException(
                        "Une programmation active existe déjà pour ce fichier");
            }
            if (scheduleRepository.countByUserIdAndActiveTrue(userId)
                    >= MAX_ACTIVE_SCHEDULES) {
                throw new QuotaExceededException(
                        "Limite de 5 programmations actives atteinte");
            }

            schedule.setActive(true);
            schedule.setConsecutiveFailures(0);
            schedule.setNextRunAt(calculateNextRunAt(schedule, Instant.now()));
            schedule = scheduleRepository.save(schedule);
            safeAudit(userId, "SCHEDULE_ACTIVATED", "SCHEDULE", schedule.getId(),
                    scheduleDetails(schedule), ipAddress);
            eventPublisher.publishEvent(new ScheduleStatusChangedEvent(
                    userId, schedule.getId(), true, false));
            log.info("Programmation {} activée", schedule.getId());
        } else {
            schedule.setActive(false);
            schedule.setNextRunAt(null);
            schedule = scheduleRepository.save(schedule);
            safeAudit(userId, "SCHEDULE_DEACTIVATED", "SCHEDULE", schedule.getId(),
                    scheduleDetails(schedule), ipAddress);
            eventPublisher.publishEvent(new ScheduleStatusChangedEvent(
                    userId, schedule.getId(), false, false));
            log.info("Programmation {} désactivée", schedule.getId());
        }

        return toResponse(schedule);
    }

    public PageResponse<ScheduleExecutionResponse> listExecutions(
            UUID userId, UUID scheduleId, Pageable pageable) {
        getOwnedSchedule(userId, scheduleId);
        Page<ScheduleExecutionEntity> page =
                executionRepository.findByScheduleId(scheduleId, pageable);
        return PageResponse.from(page, ScheduleExecutionResponse::from);
    }

    public int executeDueSchedules() {
        Instant now = Instant.now();
        List<BackupScheduleEntity> due =
                scheduleRepository.findByActiveTrueAndNextRunAtLessThanEqualOrderByNextRunAtAsc(
                        now);
        int executed = 0;

        for (BackupScheduleEntity candidate : due) {
            BackupScheduleEntity schedule =
                    scheduleRepository.findById(candidate.getId()).orElse(null);
            if (schedule == null || !schedule.isActive()) {
                continue;
            }
            if (schedule.getNextRunAt() == null
                    || schedule.getNextRunAt().isAfter(Instant.now())) {
                continue;
            }
            try {
                executeSchedule(schedule);
                executed++;
            } catch (RuntimeException exception) {
                log.error("Impossible d'exécuter la programmation {}",
                        schedule.getId(), exception);
            }
        }

        if (executed > 0) {
            log.info("{} programmation(s) exécutée(s)", executed);
        }
        return executed;
    }

    private void executeSchedule(BackupScheduleEntity schedule) {
        Instant startedAt = Instant.now();
        schedule.setLastRunAt(startedAt);
        schedule.setNextRunAt(calculateNextRunAt(schedule, startedAt));
        scheduleRepository.save(schedule);

        ScheduleExecutionStatus status;
        UUID backupId = null;
        String errorMessage = null;

        try {
            BackupService.BackupExecution result = backupService.createAndPerformBackup(
                    schedule.getUserId(), schedule.getFileId(), BackupType.SCHEDULED);
            backupId = result.backupId();

            if (result.status() == BackupStatus.SUCCESS) {
                status = ScheduleExecutionStatus.SUCCESS;
                schedule.setConsecutiveFailures(0);
            } else {
                status = ScheduleExecutionStatus.FAILED;
                errorMessage = truncate(result.errorMessage(), 2000);
                registerFailure(schedule, errorMessage);
            }
        } catch (RuntimeException exception) {
            status = ScheduleExecutionStatus.FAILED;
            errorMessage = truncate(exception.getMessage(), 2000);
            registerFailure(schedule, errorMessage);
        }

        Instant completedAt = Instant.now();
        long durationMs = Math.max(0, Duration.between(startedAt, completedAt).toMillis());

        ScheduleExecutionEntity execution = ScheduleExecutionEntity.builder()
                .scheduleId(schedule.getId())
                .backupId(backupId)
                .status(status)
                .errorMessage(errorMessage)
                .durationMs(durationMs)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .build();

        scheduleRepository.save(schedule);
        executionRepository.save(execution);

        Map<String, Object> details = new HashMap<>();
        details.put("scheduleId", schedule.getId().toString());
        details.put("durationMs", durationMs);
        if (backupId != null) {
            details.put("backupId", backupId.toString());
        }
        if (errorMessage != null) {
            details.put("reason", errorMessage);
        }

        safeAudit(
                schedule.getUserId(),
                status == ScheduleExecutionStatus.SUCCESS
                        ? "SCHEDULE_EXECUTED"
                        : "SCHEDULE_EXECUTION_FAILED",
                "SCHEDULE",
                schedule.getId(),
                details,
                null
        );

        log.info("Programmation {} exécutée avec le statut {} en {} ms",
                schedule.getId(), status, durationMs);
    }

    private void registerFailure(BackupScheduleEntity schedule, String reason) {
        int failures = schedule.getConsecutiveFailures() + 1;
        schedule.setConsecutiveFailures(failures);

        if (failures >= MAX_CONSECUTIVE_FAILURES && schedule.isActive()) {
            schedule.setActive(false);
            schedule.setNextRunAt(null);

            safeAudit(schedule.getUserId(), "SCHEDULE_AUTO_SUSPENDED", "SCHEDULE",
                    schedule.getId(),
                    Map.of(
                            "consecutiveFailures", failures,
                            "reason", truncate(reason, 500)),
                    null);

            eventPublisher.publishEvent(new ScheduleStatusChangedEvent(
                    schedule.getUserId(), schedule.getId(), false, true));

            log.warn("Programmation {} suspendue après {} échecs consécutifs",
                    schedule.getId(), failures);
        }
    }

    private ValidatedSchedule validate(
            ScheduleFrequency frequency,
            String scheduledTime,
            DayOfWeek dayOfWeek,
            String timezone
    ) {
        if (frequency == null) {
            throw new BadRequestException("frequency est requise");
        }

        LocalTime time;
        try {
            time = LocalTime.parse(scheduledTime, TIME_FORMAT);
        } catch (DateTimeException exception) {
            throw new BadRequestException("scheduledTime doit être au format HH:mm");
        }

        ZoneId zone;
        try {
            zone = ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            throw new BadRequestException("Fuseau horaire invalide");
        }

        if (frequency == ScheduleFrequency.WEEKLY && dayOfWeek == null) {
            throw new BadRequestException(
                    "dayOfWeek est requis pour une fréquence WEEKLY");
        }

        return new ValidatedSchedule(
                frequency,
                time,
                frequency == ScheduleFrequency.WEEKLY ? dayOfWeek : null,
                zone
        );
    }

    private Instant calculateNextRunAt(BackupScheduleEntity schedule, Instant from) {
        ZoneId zone;
        try {
            zone = ZoneId.of(schedule.getTimezone());
        } catch (DateTimeException exception) {
            throw new BadRequestException("Fuseau horaire invalide");
        }

        ZonedDateTime reference = from.atZone(zone);
        ZonedDateTime candidate;

        if (schedule.getFrequency() == ScheduleFrequency.DAILY) {
            candidate = reference.toLocalDate()
                    .atTime(schedule.getScheduledTime())
                    .atZone(zone);
            if (!candidate.isAfter(reference)) {
                candidate = candidate.plusDays(1);
            }
        } else {
            if (schedule.getDayOfWeek() == null) {
                throw new BadRequestException(
                        "dayOfWeek est requis pour une fréquence WEEKLY");
            }
            candidate = reference.toLocalDate()
                    .with(TemporalAdjusters.nextOrSame(schedule.getDayOfWeek()))
                    .atTime(schedule.getScheduledTime())
                    .atZone(zone);
            if (!candidate.isAfter(reference)) {
                candidate = candidate.plusWeeks(1);
            }
        }

        return candidate.toInstant();
    }

    private ScheduleResponse toResponse(BackupScheduleEntity schedule) {
        String fileName = fileRepository.findById(schedule.getFileId())
                .map(FileEntity::getOriginalName)
                .orElse(null);
        return ScheduleResponse.from(schedule, fileName);
    }

    private FileEntity getOwnedFile(UUID userId, UUID fileId) {
        FileEntity file = fileRepository.findByIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Fichier introuvable"));
        if (!file.getUserId().equals(userId)) {
            throw new AccessDeniedException("Ce fichier appartient à un autre utilisateur");
        }
        return file;
    }

    private BackupScheduleEntity getOwnedSchedule(UUID userId, UUID scheduleId) {
        BackupScheduleEntity schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Programmation introuvable"));
        if (!schedule.getUserId().equals(userId)) {
            throw new AccessDeniedException(
                    "Cette programmation appartient à un autre utilisateur");
        }
        return schedule;
    }

    private Map<UUID, String> loadFileNames(List<BackupScheduleEntity> schedules) {
        Set<UUID> fileIds = schedules.stream()
                .map(BackupScheduleEntity::getFileId)
                .collect(Collectors.toSet());
        if (fileIds.isEmpty()) {
            return Map.of();
        }
        return fileRepository.findAllById(fileIds).stream()
                .collect(Collectors.toMap(FileEntity::getId, FileEntity::getOriginalName));
    }

    private Map<String, Object> scheduleDetails(BackupScheduleEntity schedule) {
        Map<String, Object> details = new HashMap<>();
        details.put("fileId", schedule.getFileId().toString());
        details.put("frequency", schedule.getFrequency().name());
        details.put("scheduledTime", schedule.getScheduledTime().format(TIME_FORMAT));
        details.put("timezone", schedule.getTimezone());
        if (schedule.getDayOfWeek() != null) {
            details.put("dayOfWeek", schedule.getDayOfWeek().name());
        }
        if (schedule.getNextRunAt() != null) {
            details.put("nextRunAt", schedule.getNextRunAt().toString());
        }
        return details;
    }

    private void safeAudit(
            UUID userId,
            String action,
            String resourceType,
            UUID resourceId,
            Map<String, Object> details,
            String ipAddress
    ) {
        try {
            auditService.log(userId, action, resourceType, resourceId, details, ipAddress);
        } catch (RuntimeException exception) {
            log.error("Impossible d'enregistrer l'audit {} pour {}",
                    action, resourceId, exception);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "Erreur inconnue";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record ValidatedSchedule(
            ScheduleFrequency frequency,
            LocalTime scheduledTime,
            DayOfWeek dayOfWeek,
            ZoneId timezone
    ) {
    }
}
