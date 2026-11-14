package com.smartbackup.service;

import com.smartbackup.dto.response.DownloadResponse;
import com.smartbackup.dto.response.FileResponse;
import com.smartbackup.dto.response.FileSummaryResponse;
import com.smartbackup.dto.response.PageResponse;
import com.smartbackup.entity.FileEntity;
import com.smartbackup.exception.BadRequestException;
import com.smartbackup.exception.FileTooLargeException;
import com.smartbackup.exception.InvalidFileTypeException;
import com.smartbackup.exception.QuotaExceededException;
import com.smartbackup.exception.ResourceNotFoundException;
import com.smartbackup.exception.StorageException;
import com.smartbackup.repository.FileRepository;
import com.smartbackup.util.HashUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileService {

    private final FileRepository fileRepository;
    private final StorageService storageService;
    private final Tika tika = new Tika();
    private final long maxFileSizeBytes;
    private final long maxFileSizeMb;
    private final long maxFilesPerUser;
    private final Set<String> allowedMimeTypes;

    public FileService(
            FileRepository fileRepository,
            StorageService storageService,
            @Value("${app.files.max-size-mb}") long maxFileSizeMb,
            @Value("${app.files.max-per-user}") long maxFilesPerUser,
            @Value("${app.files.allowed-mime-types}") String allowedMimeTypes
    ) {
        this.fileRepository = fileRepository;
        this.storageService = storageService;
        this.maxFileSizeMb = maxFileSizeMb;
        this.maxFileSizeBytes = maxFileSizeMb * 1024 * 1024;
        this.maxFilesPerUser = maxFilesPerUser;
        this.allowedMimeTypes = Arrays.stream(allowedMimeTypes.split(","))
                .map(String::trim)
                .filter(mimeType -> !mimeType.isEmpty())
                .map(mimeType -> mimeType.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional
    public FileResponse upload(UUID userId, MultipartFile multipartFile) {
        validateUploadRequest(userId, multipartFile);

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("smartbackup-upload-", ".tmp");
            try (InputStream inputStream = multipartFile.getInputStream()) {
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            long fileSize = Files.size(tempFile);
            if (fileSize == 0) {
                throw new BadRequestException("Le fichier est vide");
            }
            if (fileSize > maxFileSizeBytes) {
                throw new FileTooLargeException(maxFileSizeMb);
            }

            String mimeType = tika.detect(tempFile.toFile()).toLowerCase(Locale.ROOT);
            if (!allowedMimeTypes.contains(mimeType)) {
                throw new InvalidFileTypeException(mimeType);
            }

            String originalName = sanitizeFilename(multipartFile.getOriginalFilename());
            String storedKey = "users/%s/files/%s-%s".formatted(userId, UUID.randomUUID(), originalName);
            String sha256Hash = HashUtil.sha256(tempFile);

            try (InputStream inputStream = Files.newInputStream(tempFile)) {
                storageService.upload(storedKey, mimeType, fileSize, inputStream);
            }

            FileEntity file = FileEntity.builder()
                    .userId(userId)
                    .originalName(originalName)
                    .storedKey(storedKey)
                    .fileSize(fileSize)
                    .mimeType(mimeType)
                    .sha256Hash(sha256Hash)
                    .deleted(false)
                    .build();

            try {
                file = fileRepository.save(file);
            } catch (RuntimeException e) {
                try {
                    storageService.delete(storedKey);
                } catch (RuntimeException cleanupException) {
                    e.addSuppressed(cleanupException);
                }
                throw e;
            }

            log.info("Fichier {} uploadé par l'utilisateur {}", file.getId(), userId);
            return FileResponse.from(file);
        } catch (IOException e) {
            throw new StorageException("Erreur lors du traitement du fichier", e);
        } finally {
            deleteTempFile(tempFile);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<FileSummaryResponse> list(UUID userId, String search, Pageable pageable) {
        Page<FileEntity> page = search == null || search.isBlank()
                ? fileRepository.findByUserIdAndDeletedFalse(userId, pageable)
                : fileRepository.findByUserIdAndDeletedFalseAndOriginalNameContainingIgnoreCase(
                        userId, search.trim(), pageable);

        return PageResponse.from(page, FileSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public FileResponse get(UUID userId, UUID fileId) {
        return FileResponse.from(getOwnedFile(userId, fileId));
    }

    @Transactional(readOnly = true)
    public DownloadResponse download(UUID userId, UUID fileId) {
        FileEntity file = getOwnedFile(userId, fileId);
        StorageService.PresignedUrl presignedUrl = storageService.generateDownloadUrl(file.getStoredKey());

        return new DownloadResponse(
                presignedUrl.url(),
                presignedUrl.expiresAt(),
                file.getOriginalName(),
                file.getFileSize(),
                file.getMimeType()
        );
    }

    @Transactional
    public void delete(UUID userId, UUID fileId) {
        FileEntity file = getOwnedFile(userId, fileId);
        file.setDeleted(true);
        log.info("Fichier {} supprimé logiquement par l'utilisateur {}", fileId, userId);
    }

    private void validateUploadRequest(UUID userId, MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BadRequestException("Aucun fichier fourni");
        }
        if (multipartFile.getSize() > maxFileSizeBytes) {
            throw new FileTooLargeException(maxFileSizeMb);
        }
        if (maxFilesPerUser > 0 && fileRepository.countByUserIdAndDeletedFalse(userId) >= maxFilesPerUser) {
            throw new QuotaExceededException(
                    "Quota de fichiers atteint (maximum : " + maxFilesPerUser + " fichiers)");
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

    private String sanitizeFilename(String originalFilename) {
        String name = originalFilename == null || originalFilename.isBlank()
                ? "file"
                : originalFilename;

        name = name.replace('\\', '/');
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }

        name = name.replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("\\.{2,}", ".");

        if (name.isBlank() || name.equals(".") || name.equals("_")) {
            name = "file";
        }

        if (name.length() > 255) {
            String extension = "";
            int dot = name.lastIndexOf('.');
            if (dot > 0 && dot < name.length() - 1) {
                extension = name.substring(dot);
            }
            int maxBaseLength = Math.max(1, 255 - extension.length());
            name = name.substring(0, Math.min(maxBaseLength, name.length())) + extension;
        }

        return name;
    }

    private void deleteTempFile(Path tempFile) {
        if (tempFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            log.warn("Impossible de supprimer le fichier temporaire {}", tempFile, e);
        }
    }
}
