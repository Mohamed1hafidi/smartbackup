package com.smartbackup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbackup.entity.BackupEntity;
import com.smartbackup.entity.BackupStatus;
import com.smartbackup.entity.BackupType;
import com.smartbackup.entity.FileEntity;
import com.smartbackup.entity.RestoreJobEntity;
import com.smartbackup.entity.RestoreStatus;
import com.smartbackup.exception.StorageException;
import com.smartbackup.repository.ActivityLogRepository;
import com.smartbackup.repository.BackupRepository;
import com.smartbackup.entity.NotificationType;
import com.smartbackup.repository.FileRepository;
import com.smartbackup.repository.NotificationRepository;
import com.smartbackup.repository.RefreshTokenRepository;
import com.smartbackup.repository.RestoreJobRepository;
import com.smartbackup.repository.UserRepository;
import com.smartbackup.service.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RestoreFlowIntegrationTest {

    private static final String FILE_NAME = "restore.txt";
    private static final String CURRENT_CONTENT = "contenu actuel";
    private static final String BACKUP_CONTENT = "contenu sauvegardé";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private BackupRepository backupRepository;

    @Autowired
    private RestoreJobRepository restoreJobRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private StorageService storageService;

    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();
    private final AtomicReference<byte[]> lastFileUpload = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        cleanDatabase();
        objects.clear();
        lastFileUpload.set(null);

        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Path destination = invocation.getArgument(1);
            byte[] content = objects.get(key);
            if (content == null) {
                throw new StorageException("Objet introuvable : " + key);
            }
            Files.write(destination, content);
            return null;
        }).when(storageService).download(anyString(), any(Path.class));

        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            InputStream inputStream = invocation.getArgument(3);
            byte[] content = inputStream.readAllBytes();
            objects.put(key, content);
            if (key.contains("/files/")) {
                lastFileUpload.set(content);
            }
            return null;
        }).when(storageService).upload(anyString(), anyString(), anyLong(), any(InputStream.class));

        when(storageService.generateDownloadUrl(anyString()))
                .thenReturn(new StorageService.PresignedUrl(
                        "https://r2.example.com/backup.zip",
                        Instant.now().plusSeconds(900)));
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
        objects.clear();
    }

    @Test
    void restoresBackupAfterCreatingSafetyBackup() throws Exception {
        String token = registerAndLogin("restore@example.com");
        String fileId = uploadFile(token);
        FileEntity file = fileRepository.findById(UUID.fromString(fileId)).orElseThrow();

        BackupEntity backup = seedBackup(file, "v1", BACKUP_CONTENT);
        byte[] currentBytes = CURRENT_CONTENT.getBytes(StandardCharsets.UTF_8);
        lastFileUpload.set(null);

        MvcResult restoreResult = mockMvc.perform(
                        post("/api/backups/{id}/restore", backup.getId())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Restauration démarrée"))
                .andExpect(jsonPath("$.data.backupId").value(backup.getId().toString()))
                .andExpect(jsonPath("$.data.fileId").value(fileId))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        String jobId = readJson(restoreResult, "data", "restoreJobId");
        awaitRestoreStatus(token, jobId, "SUCCESS");

        byte[] backupBytes = BACKUP_CONTENT.getBytes(StandardCharsets.UTF_8);
        FileEntity restoredFile = fileRepository.findById(file.getId()).orElseThrow();
        assertEquals(sha256(backupBytes), restoredFile.getSha256Hash());
        assertEquals(backupBytes.length, restoredFile.getFileSize());
        assertArrayEquals(backupBytes, lastFileUpload.get());

        assertEquals(2, backupRepository.count());
        BackupEntity safetyBackup = backupRepository.findAll().stream()
                .filter(candidate -> !candidate.getId().equals(backup.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(BackupStatus.SUCCESS, safetyBackup.getStatus());
        assertEquals(2, safetyBackup.getVersionNumber());
        assertEquals(sha256(currentBytes), safetyBackup.getSha256Hash());

        assertEquals(1, activityLogRepository.countByAction("RESTORE_SUCCESS"));
        awaitNotificationType(NotificationType.RESTORE_SUCCESS);

        mockMvc.perform(get("/api/backups/restores/{jobId}", jobId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());
    }

    @Test
    void failsRestoreWhenContentHashDoesNotMatch() throws Exception {
        String token = registerAndLogin("integrity@example.com");
        String fileId = uploadFile(token);
        FileEntity file = fileRepository.findById(UUID.fromString(fileId)).orElseThrow();

        BackupEntity backup = seedBackup(file, "v1", BACKUP_CONTENT);
        backup.setSha256Hash(sha256("autre contenu".getBytes(StandardCharsets.UTF_8)));
        backupRepository.save(backup);
        lastFileUpload.set(null);

        MvcResult restoreResult = mockMvc.perform(
                        post("/api/backups/{id}/restore", backup.getId())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andReturn();

        String jobId = readJson(restoreResult, "data", "restoreJobId");
        awaitRestoreStatus(token, jobId, "FAILED");

        assertEquals(1, backupRepository.count());
        assertNull(lastFileUpload.get());
        assertEquals(1, activityLogRepository.countByAction("RESTORE_INTEGRITY_FAIL"));
        awaitNotificationType(NotificationType.RESTORE_FAILED);

        mockMvc.perform(get("/api/backups/restores/{jobId}", jobId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.errorMessage")
                        .value("Intégrité du fichier restauré compromise"));
    }

    @Test
    void rejectsRestoreOfNonSuccessfulBackup() throws Exception {
        String token = registerAndLogin("pending@example.com");
        FileEntity file = fileRepository.findById(UUID.fromString(uploadFile(token))).orElseThrow();
        BackupEntity backup = seedBackup(file, "v1", BACKUP_CONTENT);
        backup.setStatus(BackupStatus.PENDING);
        backup.setCompletedAt(null);
        backupRepository.save(backup);

        mockMvc.perform(post("/api/backups/{id}/restore", backup.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("La sauvegarde n'est pas dans un état SUCCESS"));

        assertEquals(0, restoreJobRepository.count());
    }

    @Test
    void rejectsRestoreWhenAnotherRestoreIsInProgress() throws Exception {
        String token = registerAndLogin("restore-conflict@example.com");
        FileEntity file = fileRepository.findById(UUID.fromString(uploadFile(token))).orElseThrow();
        BackupEntity backup = seedBackup(file, "v1", BACKUP_CONTENT);

        restoreJobRepository.save(RestoreJobEntity.builder()
                .userId(file.getUserId())
                .backupId(backup.getId())
                .fileId(file.getId())
                .status(RestoreStatus.PENDING)
                .build());

        mockMvc.perform(post("/api/backups/{id}/restore", backup.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Une restauration est déjà en cours pour ce fichier"));
    }

    @Test
    void rejectsRestoreOfAnotherUsersBackup() throws Exception {
        String ownerToken = registerAndLogin("restore-owner@example.com");
        FileEntity file = fileRepository.findById(
                UUID.fromString(uploadFile(ownerToken))).orElseThrow();
        BackupEntity backup = seedBackup(file, "v1", BACKUP_CONTENT);

        String intruderToken = registerAndLogin("restore-intruder@example.com");

        mockMvc.perform(post("/api/backups/{id}/restore", backup.getId())
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Cette sauvegarde appartient à un autre utilisateur"));
    }

    @Test
    void rejectsUnauthenticatedRestore() throws Exception {
        mockMvc.perform(post("/api/backups/{id}/restore", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsPresignedDownloadUrlForSuccessfulBackup() throws Exception {
        String token = registerAndLogin("download@example.com");
        FileEntity file = fileRepository.findById(UUID.fromString(uploadFile(token))).orElseThrow();
        BackupEntity backup = seedBackup(file, "v1", BACKUP_CONTENT);

        mockMvc.perform(get("/api/backups/{id}/download", backup.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.downloadUrl")
                        .value("https://r2.example.com/backup.zip"))
                .andExpect(jsonPath("$.data.fileName").value(FILE_NAME + ".zip"))
                .andExpect(jsonPath("$.data.mimeType").value("application/zip"))
                .andExpect(jsonPath("$.data.expiresAt").isNotEmpty());

        verify(storageService).generateDownloadUrl(backup.getR2ObjectKey());
    }

    private BackupEntity seedBackup(FileEntity file, String suffix, String content)
            throws IOException {
        String key = "users/%s/backups/%s/%s.zip".formatted(
                file.getUserId(), file.getId(), suffix);
        byte[] zipBytes = zip(FILE_NAME, content.getBytes(StandardCharsets.UTF_8));
        objects.put(key, zipBytes);

        return backupRepository.save(BackupEntity.builder()
                .userId(file.getUserId())
                .fileId(file.getId())
                .r2ObjectKey(key)
                .r2Bucket("test-bucket")
                .fileSize(zipBytes.length)
                .sha256Hash(sha256(content.getBytes(StandardCharsets.UTF_8)))
                .archiveSha256Hash(sha256(zipBytes))
                .backupType(BackupType.MANUAL)
                .status(BackupStatus.SUCCESS)
                .versionNumber(1)
                .createdAt(Instant.now().minusSeconds(60))
                .completedAt(Instant.now().minusSeconds(60))
                .build());
    }

    private byte[] zip(String entryName, byte[] content) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            zipOutputStream.write(content);
            zipOutputStream.closeEntry();
        }
        return outputStream.toByteArray();
    }

    private String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void awaitRestoreStatus(String token, String jobId, String expectedStatus)
            throws Exception {
        String lastStatus = null;
        for (int attempt = 0; attempt < 100; attempt++) {
            MvcResult result = mockMvc.perform(
                            get("/api/backups/restores/{jobId}", jobId)
                                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();
            lastStatus = readJson(result, "data", "status");
            if (expectedStatus.equals(lastStatus)) {
                return;
            }
            Thread.sleep(50);
        }
        fail("Statut attendu " + expectedStatus + " mais obtenu " + lastStatus);
    }

    private void awaitNotificationType(NotificationType type) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (notificationRepository.findAll().stream()
                    .anyMatch(notification -> notification.getType() == type)) {
                return;
            }
            Thread.sleep(50);
        }
        fail("Notification attendue non créée : " + type);
    }

    private String uploadFile(String token) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                FILE_NAME,
                MediaType.TEXT_PLAIN_VALUE,
                CURRENT_CONTENT.getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc.perform(multipart("/api/files")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();

        return readJson(result, "data", "id");
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Secure@123",
                                "fullName", "Restore User"))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Secure@123"))))
                .andExpect(status().isOk())
                .andReturn();

        return readJson(loginResult, "data", "accessToken");
    }

    private String readJson(MvcResult result, String... path) throws Exception {
        var node = objectMapper.readTree(result.getResponse().getContentAsString());
        for (String segment : path) {
            node = node.get(segment);
        }
        return node.asText();
    }

    private void cleanDatabase() {
        notificationRepository.deleteAll();
        restoreJobRepository.deleteAll();
        activityLogRepository.deleteAll();
        backupRepository.deleteAll();
        fileRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }
}
