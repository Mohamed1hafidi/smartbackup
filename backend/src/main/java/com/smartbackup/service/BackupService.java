package com.smartbackup.service;

import com.smartbackup.config.StorageProperties;
import com.smartbackup.dto.response.BackupResponse;
import com.smartbackup.dto.response.BackupSummaryResponse;
import com.smartbackup.dto.response.PageResponse;
import com.smartbackup.entity.BackupEntity;
import com.smartbackup.entity.BackupStatus;
import com.smartbackup.entity.BackupType;
import com.smartbackup.entity.FileEntity;
import com.smartbackup.exception.BackupInProgressException;
import com.smartbackup.exception.BadRequestException;
import com.smartbackup.exception.ResourceNotFoundException;
import com.smartbackup.event.BackupCompletedEvent;
import com.smartbackup.exception.StorageException;
import com.smartbackup.repository.BackupRepository;
import com.smartbackup.repository.FileRepository;
import com.smartbackup.util.HashUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class BackupService {

    private static final int MAX_VERSIONS_PER_FILE = 10;

    private final BackupRepository backupRepository;
    private final FileRepository fileRepository;
    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final TaskExecutor backupTaskExecutor;
    private final ApplicationEventPublisher eventPublisher;

    public BackupService(
            BackupRepository backupRepository,
            FileRepository fileRepository,
            StorageService storageService,
            StorageProperties storageProperties,
            @Qualifier("backupTaskExecutor") TaskExecutor backupTaskExecutor,
            ApplicationEventPublisher eventPublisher
    ) {
        this.backupRepository = backupRepository;
        this.fileRepository = fileRepository;
        this.storageService = storageService;
        this.storageProperties = storageProperties;
        this.backupTaskExecutor = backupTaskExecutor;
        this.eventPublisher = eventPublisher;
    }

    public BackupResponse initiateManualBackup(UUID userId, UUID fileId) {
        FileEntity file = getOwnedFile(userId, fileId);
        BackupEntity backup = createBackupRecord(file, BackupType.MANUAL);
        UUID backupId = backup.getId();

        backupTaskExecutor.execute(() -> performBackup(backupId));

        log.info("Sauvegarde manuelle {} planifiée pour le fichier {} (version {})",
                backupId, fileId, backup.getVersionNumber());
        return BackupResponse.from(backup, file.getOriginalName());
    }

    public BackupStatus createAndPerformSafetyBackup(UUID userId, UUID fileId) {
        log.info("Création d'une sauvegarde de sécurité pour le fichier {}", fileId);
        return createAndPerformBackup(userId, fileId, BackupType.MANUAL).status();
    }

    public BackupExecution createAndPerformBackup(
            UUID userId, UUID fileId, BackupType backupType) {
        FileEntity file = getOwnedFile(userId, fileId);
        BackupEntity backup = createBackupRecord(file, backupType);
        BackupStatus status = performBackup(backup.getId());
        BackupEntity completed = backupRepository.findById(backup.getId()).orElse(backup);

        return new BackupExecution(
                backup.getId(), status, completed.getErrorMessage());
    }

    private BackupEntity createBackupRecord(FileEntity file, BackupType backupType) {
        UUID fileId = file.getId();
        if (backupRepository.existsByFileIdAndStatusIn(
                fileId, List.of(BackupStatus.PENDING, BackupStatus.IN_PROGRESS))) {
            throw new BackupInProgressException("Une sauvegarde est déjà en cours pour ce fichier");
        }

        int versionNumber = backupRepository.findMaxVersionNumberByFileId(fileId) + 1;
        String objectKey = "users/%s/backups/%s/v%d/%d.zip".formatted(
                file.getUserId(), fileId, versionNumber, Instant.now().toEpochMilli());

        BackupEntity backup = BackupEntity.builder()
                .userId(file.getUserId())
                .fileId(fileId)
                .r2ObjectKey(objectKey)
                .r2Bucket(storageProperties.bucket())
                .fileSize(file.getFileSize())
                .sha256Hash(file.getSha256Hash())
                .backupType(backupType)
                .status(BackupStatus.PENDING)
                .versionNumber(versionNumber)
                .build();

        return backupRepository.save(backup);
    }

    @Transactional(readOnly = true)
    public PageResponse<BackupSummaryResponse> list(
            UUID userId,
            BackupStatus status,
            UUID fileId,
            Instant dateFrom,
            Instant dateTo,
            Pageable pageable
    ) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BadRequestException("dateFrom doit être antérieure ou égale à dateTo");
        }

        Specification<BackupEntity> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (fileId != null) {
                predicates.add(criteriaBuilder.equal(root.get("fileId"), fileId));
            }
            if (dateFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<BackupEntity> page = backupRepository.findAll(specification, pageable);
        Map<UUID, String> fileNames = loadFileNames(page.getContent());

        return PageResponse.from(page,
                backup -> BackupSummaryResponse.from(backup, fileNames.get(backup.getFileId())));
    }

    @Transactional(readOnly = true)
    public BackupResponse get(UUID userId, UUID backupId) {
        BackupEntity backup = getOwnedBackup(userId, backupId);
        String fileName = fileRepository.findById(backup.getFileId())
                .map(FileEntity::getOriginalName)
                .orElse(null);
        return BackupResponse.from(backup, fileName);
    }

    private BackupStatus performBackup(UUID backupId) {
        BackupEntity backup = backupRepository.findById(backupId).orElse(null);
        if (backup == null) {
            return BackupStatus.FAILED;
        }
        if (backup.getStatus() != BackupStatus.PENDING) {
            return backup.getStatus();
        }

        backup.setStatus(BackupStatus.IN_PROGRESS);
        backupRepository.save(backup);

        Path sourceFile = null;
        Path zipFile = null;
        FileEntity file = null;

        try {
            file = fileRepository.findById(backup.getFileId())
                    .orElseThrow(() -> new StorageException("Fichier source introuvable"));

            sourceFile = Files.createTempFile("smartbackup-backup-src-", ".tmp");
            zipFile = Files.createTempFile("smartbackup-backup-", ".zip");

            storageService.download(file.getStoredKey(), sourceFile);

            String sourceHash = HashUtil.sha256(sourceFile);
            if (!sourceHash.equalsIgnoreCase(file.getSha256Hash())) {
                throw new StorageException("Intégrité du fichier source compromise");
            }

            compress(sourceFile, file.getOriginalName(), zipFile);

            long zipSize = Files.size(zipFile);
            String zipHash = HashUtil.sha256(zipFile);

            try (InputStream inputStream = Files.newInputStream(zipFile)) {
                storageService.upload(backup.getR2ObjectKey(), "application/zip", zipSize, inputStream);
            }

            backup.setFileSize(zipSize);
            backup.setArchiveSha256Hash(zipHash);
            backup.setStatus(BackupStatus.SUCCESS);
            backup.setCompletedAt(Instant.now());
            backupRepository.save(backup);

            rotateOldVersions(backup.getFileId());

            publishBackupEvent(backup, file, BackupStatus.SUCCESS, null);

            log.info("Sauvegarde {} terminée (version {}, {} octets)",
                    backupId, backup.getVersionNumber(), zipSize);
            return BackupStatus.SUCCESS;
        } catch (Exception exception) {
            log.error("Sauvegarde {} échouée", backupId, exception);
            backup.setStatus(BackupStatus.FAILED);
            backup.setCompletedAt(Instant.now());
            backup.setErrorMessage(truncate(exception.getMessage(), 2000));
            backupRepository.save(backup);
            publishBackupEvent(backup, file, BackupStatus.FAILED, backup.getErrorMessage());
            return BackupStatus.FAILED;
        } finally {
            deleteTempFile(sourceFile);
            deleteTempFile(zipFile);
        }
    }

    private void publishBackupEvent(
            BackupEntity backup, FileEntity file, BackupStatus status, String errorMessage) {
        eventPublisher.publishEvent(new BackupCompletedEvent(
                backup.getUserId(),
                backup.getFileId(),
                backup.getId(),
                file != null ? file.getOriginalName() : null,
                backup.getVersionNumber(),
                status,
                errorMessage
        ));
    }

    private void rotateOldVersions(UUID fileId) {
        while (backupRepository.countByFileIdAndStatus(fileId, BackupStatus.SUCCESS) > MAX_VERSIONS_PER_FILE) {
            BackupEntity oldest = backupRepository
                    .findFirstByFileIdAndStatusOrderByVersionNumberAsc(fileId, BackupStatus.SUCCESS)
                    .orElse(null);
            if (oldest == null) {
                return;
            }

            backupRepository.delete(oldest);
            try {
                storageService.delete(oldest.getR2ObjectKey());
            } catch (RuntimeException exception) {
                log.warn("Impossible de supprimer l'ancienne sauvegarde {} du stockage",
                        oldest.getId(), exception);
            }
            log.info("Ancienne sauvegarde {} (version {}) supprimée par rotation",
                    oldest.getId(), oldest.getVersionNumber());
        }
    }

    private FileEntity getOwnedFile(UUID userId, UUID fileId) {
        FileEntity file = fileRepository.findByIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Fichier introuvable"));

        if (!file.getUserId().equals(userId)) {
            throw new AccessDeniedException("Ce fichier appartient à un autre utilisateur");
        }
        return file;
    }

    private BackupEntity getOwnedBackup(UUID userId, UUID backupId) {
        BackupEntity backup = backupRepository.findById(backupId)
                .orElseThrow(() -> new ResourceNotFoundException("Sauvegarde introuvable"));

        if (!backup.getUserId().equals(userId)) {
            throw new AccessDeniedException("Cette sauvegarde appartient à un autre utilisateur");
        }
        return backup;
    }

    private Map<UUID, String> loadFileNames(List<BackupEntity> backups) {
        Set<UUID> fileIds = backups.stream()
                .map(BackupEntity::getFileId)
                .collect(Collectors.toSet());

        if (fileIds.isEmpty()) {
            return Map.of();
        }

        return fileRepository.findAllById(fileIds).stream()
                .collect(Collectors.toMap(FileEntity::getId, FileEntity::getOriginalName));
    }

    private void compress(Path source, String entryName, Path destination) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(destination))) {
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            Files.copy(source, zipOutputStream);
            zipOutputStream.closeEntry();
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

    public record BackupExecution(UUID backupId, BackupStatus status, String errorMessage) {
    }
}
