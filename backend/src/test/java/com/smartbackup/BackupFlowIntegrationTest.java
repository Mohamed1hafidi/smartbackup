package com.smartbackup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbackup.entity.BackupEntity;
import com.smartbackup.entity.BackupStatus;
import com.smartbackup.entity.BackupType;
import com.smartbackup.entity.FileEntity;
import com.smartbackup.exception.StorageException;
import com.smartbackup.repository.BackupRepository;
import com.smartbackup.repository.FileRepository;
import com.smartbackup.repository.RefreshTokenRepository;
import com.smartbackup.repository.UserRepository;
import com.smartbackup.service.StorageService;
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackupFlowIntegrationTest {

    private static final String FILE_CONTENT = "contenu à sauvegarder";

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

    @MockBean
    private StorageService storageService;

    @BeforeEach
    void setUp() throws Exception {
        backupRepository.deleteAll();
        fileRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        doAnswer(invocation -> {
            Path destination = invocation.getArgument(1);
            Files.writeString(destination, FILE_CONTENT);
            return null;
        }).when(storageService).download(anyString(), any(Path.class));
    }

    @Test
    void createsManualBackupAndListsHistory() throws Exception {
        String token = registerAndLogin("backup@example.com");
        String fileId = uploadFile(token);

        MvcResult createResult = mockMvc.perform(post("/api/backups/{fileId}", fileId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Sauvegarde démarrée"))
                .andExpect(jsonPath("$.data.fileId").value(fileId))
                .andExpect(jsonPath("$.data.fileName").value("sauvegarde.txt"))
                .andExpect(jsonPath("$.data.backupType").value("MANUAL"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.versionNumber").value(1))
                .andReturn();

        String backupId = readJson(createResult, "data", "id");
        awaitStatus(token, backupId, "SUCCESS");

        mockMvc.perform(get("/api/backups/{id}", backupId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.fileSize").isNumber())
                .andExpect(jsonPath("$.data.sha256Hash").isNotEmpty())
                .andExpect(jsonPath("$.data.r2ObjectKey").value(org.hamcrest.Matchers.containsString("/backups/")))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());

        mockMvc.perform(get("/api/backups")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(backupId))
                .andExpect(jsonPath("$.data.content[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].fileName").value("sauvegarde.txt"));

        mockMvc.perform(get("/api/backups")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "SUCCESS")
                        .param("fileId", fileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/backups")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void marksBackupAsFailedWhenStorageFails() throws Exception {
        String token = registerAndLogin("failed@example.com");
        String fileId = uploadFile(token);

        doThrow(new StorageException("Stockage indisponible"))
                .when(storageService).download(anyString(), any(Path.class));

        MvcResult createResult = mockMvc.perform(post("/api/backups/{fileId}", fileId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andReturn();

        String backupId = readJson(createResult, "data", "id");
        awaitStatus(token, backupId, "FAILED");

        mockMvc.perform(get("/api/backups/{id}", backupId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.errorMessage").value("Stockage indisponible"));
    }

    @Test
    void rejectsBackupWhenAnotherIsInProgress() throws Exception {
        String token = registerAndLogin("conflict@example.com");
        String fileId = uploadFile(token);
        FileEntity file = fileRepository.findById(java.util.UUID.fromString(fileId)).orElseThrow();

        backupRepository.save(BackupEntity.builder()
                .userId(file.getUserId())
                .fileId(file.getId())
                .r2ObjectKey("users/test/backups/in-progress.zip")
                .r2Bucket("test-bucket")
                .fileSize(file.getFileSize())
                .sha256Hash(file.getSha256Hash())
                .backupType(BackupType.MANUAL)
                .status(BackupStatus.PENDING)
                .versionNumber(1)
                .build());

        mockMvc.perform(post("/api/backups/{fileId}", fileId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Une sauvegarde est déjà en cours pour ce fichier"));
    }

    @Test
    void rejectsAccessToAnotherUsersFileAndBackup() throws Exception {
        String ownerToken = registerAndLogin("owner@example.com");
        String fileId = uploadFile(ownerToken);

        MvcResult createResult = mockMvc.perform(post("/api/backups/{fileId}", fileId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isAccepted())
                .andReturn();
        String backupId = readJson(createResult, "data", "id");
        awaitStatus(ownerToken, backupId, "SUCCESS");

        String intruderToken = registerAndLogin("intruder@example.com");

        mockMvc.perform(post("/api/backups/{fileId}", fileId)
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Ce fichier appartient à un autre utilisateur"));

        mockMvc.perform(get("/api/backups/{id}", backupId)
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Cette sauvegarde appartient à un autre utilisateur"));
    }

    @Test
    void rejectsInvalidFilters() throws Exception {
        String token = registerAndLogin("filters@example.com");

        mockMvc.perform(get("/api/backups")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/backups")
                        .header("Authorization", "Bearer " + token)
                        .param("dateFrom", "not-a-date"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/backups")
                        .header("Authorization", "Bearer " + token)
                        .param("dateFrom", "2024-02-02T00:00:00Z")
                        .param("dateTo", "2024-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void keepsOnlyTenVersionsPerFile() throws Exception {
        String token = registerAndLogin("rotation@example.com");
        String fileId = uploadFile(token);
        FileEntity file = fileRepository.findById(java.util.UUID.fromString(fileId)).orElseThrow();

        for (int version = 1; version <= 10; version++) {
            backupRepository.save(BackupEntity.builder()
                    .userId(file.getUserId())
                    .fileId(file.getId())
                    .r2ObjectKey("users/test/backups/old-v" + version + ".zip")
                    .r2Bucket("test-bucket")
                    .fileSize(10)
                    .sha256Hash("a".repeat(64))
                    .backupType(BackupType.MANUAL)
                    .status(BackupStatus.SUCCESS)
                    .versionNumber(version)
                    .createdAt(Instant.now().minusSeconds(3600L - version))
                    .completedAt(Instant.now().minusSeconds(3600L - version))
                    .build());
        }

        MvcResult createResult = mockMvc.perform(post("/api/backups/{fileId}", fileId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.versionNumber").value(11))
                .andReturn();

        String backupId = readJson(createResult, "data", "id");
        awaitStatus(token, backupId, "SUCCESS");
        awaitBackupCount(file.getId(), 10);

        verify(storageService).delete("users/test/backups/old-v1.zip");
    }

    @Test
    void rejectsUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/backups"))
                .andExpect(status().isUnauthorized());
    }

    private String awaitStatus(String token, String backupId, String expectedStatus) throws Exception {
        String lastStatus = null;
        for (int attempt = 0; attempt < 100; attempt++) {
            MvcResult result = mockMvc.perform(get("/api/backups/{id}", backupId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();
            lastStatus = readJson(result, "data", "status");
            if (expectedStatus.equals(lastStatus)) {
                return lastStatus;
            }
            Thread.sleep(50);
        }
        fail("Statut attendu " + expectedStatus + " mais obtenu " + lastStatus);
        return lastStatus;
    }

    private void awaitBackupCount(java.util.UUID fileId, int expectedCount) throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            long count = backupRepository.countByFileIdAndStatus(fileId, BackupStatus.SUCCESS);
            if (count == expectedCount) {
                return;
            }
            Thread.sleep(50);
        }
        assertEquals(expectedCount,
                backupRepository.countByFileIdAndStatus(fileId, BackupStatus.SUCCESS));
    }

    private String uploadFile(String token) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sauvegarde.txt",
                MediaType.TEXT_PLAIN_VALUE,
                FILE_CONTENT.getBytes(StandardCharsets.UTF_8));

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
                                "fullName", "Backup User"))))
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
}
