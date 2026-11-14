package com.smartbackup.service;

import com.smartbackup.dto.response.DownloadResponse;
import com.smartbackup.dto.response.RestoreResponse;
import com.smartbackup.entity.BackupEntity;
import com.smartbackup.entity.BackupStatus;
import com.smartbackup.entity.FileEntity;
import com.smartbackup.entity.RestoreJobEntity;
import com.smartbackup.entity.RestoreStatus;
import com.smartbackup.event.RestoreCompletedEvent;
import com.smartbackup.exception.BackupInProgressException;
import com.smartbackup.exception.BackupIntegrityException;
import com.smartbackup.exception.BadRequestException;
import com.smartbackup.exception.ResourceNotFoundException;
import com.smartbackup.exception.RestoreInProgressException;
import com.smartbackup.exception.StorageException;
import com.smartbackup.repository.BackupRepository;
import com.smartbackup.repository.FileRepository;
import com.smartbackup.repository.RestoreJobRepository;
import com.smartbackup.util.HashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class RestoreService {

    private static final Set<RestoreStatus> ACTIVE_RESTORE_STATUSES =
            EnumSet.of(RestoreStatus.PENDING, RestoreStatus.IN_PROGRESS);

    private final BackupRepository backupRepository;
    private final FileRepository fileRepository;
    private final RestoreJobRepository restoreJobRepository;
    private final StorageService storageService;
    private final BackupService backupService;
    private final AuditService auditService;
    private final TaskExecutor backupTaskExecutor;
    private final ApplicationEventPublisher eventPublisher;

    public RestoreService(
            BackupRepository backupRepository,
            FileRepository fileRepository,
            RestoreJobRepository restoreJobRepository,
            StorageService storageService,
            BackupService backupService,
            AuditService auditService,
            @Qualifier("backupTaskExecutor") TaskExecutor backupTaskExecutor,
            ApplicationEventPublisher eventPublisher
    ) {
        this.backupRepository = backupRepository;
        this.fileRepository = fileRepository;
        this.restoreJobRepository = restoreJobRepository;
        this.storageService = storageService;
        this.backupService = backupService;
        this.auditService = auditService;
        this.backupTaskExecutor = backupTaskExecutor;
        this.eventPublisher = eventPublisher;
    }

    public RestoreResponse initiateRestore(UUID userId, UUID backupId, String ipAddress) {
        BackupEntity backup = getOwnedBackup(userId, backupId);
        if (backup.getStatus() != BackupStatus.SUCCESS) {
            throw new BadRequestException("La sauvegarde n'est pas dans un état SUCCESS");
        }

        FileEntity file = fileRepository.findByIdAndDeletedFalse(backup.getFileId())
                .orElseThrow(() -> new ResourceNotFoundException("Fichier introuvable"));

        if (restoreJobRepository.existsByFileIdAndStatusIn(file.getId(), ACTIVE_RESTORE_STATUSES)) {
            throw new RestoreInProgressException(
                    "Une restauration est déjà en cours pour ce fichier");
        }
        if (backupRepository.existsByFileIdAndStatusIn(
                file.getId(), List.of(BackupStatus.PENDING, BackupStatus.IN_PROGRESS))) {
            throw new BackupInProgressException("Une sauvegarde est déjà en cours pour ce fichier");
        }

        RestoreJobEntity job = restoreJobRepository.save(RestoreJobEntity.builder()
                .userId(userId)
                .backupId(backup.getId())
                .fileId(file.getId())
                .status(RestoreStatus.PENDING)
                .ipAddress(ipAddress)
                .build());

        safeAudit(userId, "RESTORE_REQUESTED", "BACKUP", backup.getId(),
                Map.of(
                        "restoreJobId", job.getId().toString(),
                        "fileId", file.getId().toString(),
                        "versionNumber", backup.getVersionNumber()),
                ipAddress);

        UUID jobId = job.getId();
        backupTaskExecutor.execute(() -> performRestore(jobId));

        log.info("Restauration {} planifiée pour la sauvegarde {} du fichier {}",
                jobId, backupId, file.getId());
        return RestoreResponse.from(job);
    }

    public RestoreResponse getRestore(UUID userId, UUID jobId) {
        RestoreJobEntity job = restoreJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Restauration introuvable"));

        if (!job.getUserId().equals(userId)) {
            throw new AccessDeniedException("Cette restauration appartient à un autre utilisateur");
        }
        return RestoreResponse.from(job);
    }

    public DownloadResponse getBackupDownloadUrl(UUID userId, UUID backupId) {
        BackupEntity backup = getOwnedBackup(userId, backupId);
        if (backup.getStatus() != BackupStatus.SUCCESS) {
            throw new BadRequestException("La sauvegarde n'est pas dans un état SUCCESS");
        }

        String originalName = fileRepository.findById(backup.getFileId())
                .map(FileEntity::getOriginalName)
                .orElse("sauvegarde");
        StorageService.PresignedUrl presignedUrl =
                storageService.generateDownloadUrl(backup.getR2ObjectKey());

        return new DownloadResponse(
                presignedUrl.url(),
                presignedUrl.expiresAt(),
                originalName + ".zip",
                backup.getFileSize(),
                "application/zip"
        );
    }

    private void performRestore(UUID jobId) {
        RestoreJobEntity job = restoreJobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != RestoreStatus.PENDING) {
            return;
        }

        job.setStatus(RestoreStatus.IN_PROGRESS);
        restoreJobRepository.save(job);

        Path zipFile = null;
        Path extractedFile = null;
        FileEntity file = null;

        try {
            BackupEntity backup = backupRepository.findById(job.getBackupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sauvegarde introuvable"));
            file = fileRepository.findByIdAndDeletedFalse(job.getFileId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fichier introuvable"));

            zipFile = Files.createTempFile("smartbackup-restore-", ".zip");
            extractedFile = Files.createTempFile("smartbackup-restore-", ".tmp");

            storageService.download(backup.getR2ObjectKey(), zipFile);

            String archiveHash = HashUtil.sha256(zipFile);
            String expectedArchiveHash = backup.getArchiveSha256Hash();
            boolean legacyBackup = expectedArchiveHash == null
                    || expectedArchiveHash.equalsIgnoreCase(backup.getSha256Hash());

            if (expectedArchiveHash != null
                    && !archiveHash.equalsIgnoreCase(expectedArchiveHash)) {
                throw new BackupIntegrityException("Intégrité de la sauvegarde compromise");
            }

            extractSingleEntry(zipFile, extractedFile);

            String contentHash = HashUtil.sha256(extractedFile);
            if (!legacyBackup && !contentHash.equalsIgnoreCase(backup.getSha256Hash())) {
                throw new BackupIntegrityException(
                        "Intégrité du fichier restauré compromise");
            }

            BackupStatus safetyStatus =
                    backupService.createAndPerformSafetyBackup(job.getUserId(), job.getFileId());
            if (safetyStatus != BackupStatus.SUCCESS) {
                throw new StorageException("Impossible de créer la sauvegarde de sécurité");
            }

            long contentSize = Files.size(extractedFile);
            try (InputStream inputStream = Files.newInputStream(extractedFile)) {
                storageService.upload(
                        file.getStoredKey(), file.getMimeType(), contentSize, inputStream);
            }

            file.setFileSize(contentSize);
            file.setSha256Hash(contentHash);
            file.setDeleted(false);
            fileRepository.save(file);

            job.setStatus(RestoreStatus.SUCCESS);
            job.setCompletedAt(Instant.now());
            job.setErrorMessage(null);
            restoreJobRepository.save(job);

            safeAudit(job.getUserId(), "RESTORE_SUCCESS", "FILE", file.getId(),
                    Map.of(
                            "restoreJobId", job.getId().toString(),
                            "backupId", backup.getId().toString(),
                            "versionNumber", backup.getVersionNumber(),
                            "sha256Hash", contentHash,
                            "fileSize", contentSize),
                    job.getIpAddress());

            publishRestoreEvent(job, file, RestoreStatus.SUCCESS, null);

            log.info("Restauration {} terminée pour le fichier {} (sauvegarde {}, version {})",
                    jobId, file.getId(), backup.getId(), backup.getVersionNumber());
        } catch (BackupIntegrityException exception) {
            markFailed(job, exception.getMessage());
            safeAudit(job.getUserId(), "RESTORE_INTEGRITY_FAIL", "BACKUP", job.getBackupId(),
                    Map.of(
                            "restoreJobId", job.getId().toString(),
                            "reason", truncate(exception.getMessage(), 500)),
                    job.getIpAddress());
            publishRestoreEvent(job, file, RestoreStatus.FAILED, exception.getMessage());
            log.warn("Restauration {} échouée : {}", jobId, exception.getMessage());
        } catch (Exception exception) {
            markFailed(job, exception.getMessage());
            safeAudit(job.getUserId(), "RESTORE_FAILED", "BACKUP", job.getBackupId(),
                    Map.of(
                            "restoreJobId", job.getId().toString(),
                            "reason", truncate(exception.getMessage(), 500)),
                    job.getIpAddress());
            publishRestoreEvent(job, file, RestoreStatus.FAILED, exception.getMessage());
            log.error("Restauration {} échouée", jobId, exception);
        } finally {
            deleteTempFile(zipFile);
            deleteTempFile(extractedFile);
        }
    }

    private void publishRestoreEvent(
            RestoreJobEntity job, FileEntity file, RestoreStatus status, String errorMessage) {
        eventPublisher.publishEvent(new RestoreCompletedEvent(
                job.getUserId(),
                job.getFileId(),
                job.getBackupId(),
                file != null ? file.getOriginalName() : null,
                status,
                errorMessage
        ));
    }

    private void markFailed(RestoreJobEntity job, String message) {
        job.setStatus(RestoreStatus.FAILED);
        job.setCompletedAt(Instant.now());
        job.setErrorMessage(truncate(message, 2000));
        restoreJobRepository.save(job);
    }

    private void extractSingleEntry(Path zipFile, Path destination) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry = zipInputStream.getNextEntry();
            if (entry == null || entry.isDirectory()) {
                throw new BackupIntegrityException("Archive de sauvegarde invalide");
            }

            try (OutputStream outputStream = Files.newOutputStream(destination)) {
                zipInputStream.transferTo(outputStream);
            }
            zipInputStream.closeEntry();
        }
    }

    private BackupEntity getOwnedBackup(UUID userId, UUID backupId) {
        BackupEntity backup = backupRepository.findById(backupId)
                .orElseThrow(() -> new ResourceNotFoundException("Sauvegarde introuvable"));

        if (!backup.getUserId().equals(userId)) {
            throw new AccessDeniedException("Cette sauvegarde appartient à un autre utilisateur");
        }
        return backup;
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
            log.error("Impossible d'enregistrer l'audit {} pour {}", action, resourceId, exception);
        }
    }

    private void deleteTempFile(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.warn("Impossible de supprimer le fichier temporaire {}", path, exception);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "Erreur inconnue";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
