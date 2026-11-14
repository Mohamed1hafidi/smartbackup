package com.smartbackup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbackup.entity.BackupEntity;
import com.smartbackup.entity.BackupScheduleEntity;
import com.smartbackup.entity.BackupStatus;
import com.smartbackup.entity.FileEntity;
import com.smartbackup.entity.NotificationEntity;
import com.smartbackup.entity.NotificationType;
import com.smartbackup.repository.ActivityLogRepository;
import com.smartbackup.repository.BackupRepository;
import com.smartbackup.repository.BackupScheduleRepository;
import com.smartbackup.repository.FileRepository;
import com.smartbackup.repository.NotificationRepository;
import com.smartbackup.repository.RefreshTokenRepository;
import com.smartbackup.repository.RestoreJobRepository;
import com.smartbackup.repository.ScheduleExecutionRepository;
import com.smartbackup.repository.UserRepository;
import com.smartbackup.service.ScheduleService;
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardNotificationIntegrationTest {

    private static final String CONTENT = "contenu phase 7";
    private static final String EMAIL = "phase7@example.com";
    private static final String PASSWORD = "Secure@123";

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
    private BackupScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleExecutionRepository executionRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private RestoreJobRepository restoreJobRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ScheduleService scheduleService;

    @MockBean
    private StorageService storageService;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        doAnswer(invocation -> {
            Path destination = invocation.getArgument(1);
            Files.writeString(destination, CONTENT);
            return null;
        }).when(storageService).download(anyString(), any(Path.class));
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void returnsDashboardStatsAndUpdatesThemAfterUploadsAndBackups() throws Exception {
        String token = registerAndLogin(EMAIL);

        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalFiles").value(0))
                .andExpect(jsonPath("$.data.totalBackups").value(0))
                .andExpect(jsonPath("$.data.usedStorageBytes").value(0))
                .andExpect(jsonPath("$.data.usedStorageFormatted").value("0 B"))
                .andExpect(jsonPath("$.data.successCount").value(0))
                .andExpect(jsonPath("$.data.failedCount").value(0))
                .andExpect(jsonPath("$.data.successRate").value(0.0))
                .andExpect(jsonPath("$.data.activeSchedules").value(0))
                .andExpect(jsonPath("$.data.lastBackupDate").value(nullValue()))
                .andExpect(jsonPath("$.data.nextScheduledBackup").value(nullValue()));

        String fileId = uploadFile(token, "dashboard-ok.txt");
        String failingFileId = uploadFile(token, "dashboard-fail.txt");

        FileEntity failingFile = fileRepository.findById(UUID.fromString(failingFileId))
                .orElseThrow();
        failingFile.setSha256Hash("0".repeat(64));
        fileRepository.save(failingFile);

        initiateBackup(token, fileId);
        initiateBackup(token, failingFileId);

        waitForBackupStatus(UUID.fromString(fileId), BackupStatus.SUCCESS);
        waitForBackupStatus(UUID.fromString(failingFileId), BackupStatus.FAILED);

        long expectedBytes = CONTENT.getBytes(StandardCharsets.UTF_8).length * 2L;

        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalFiles").value(2))
                .andExpect(jsonPath("$.data.totalBackups").value(2))
                .andExpect(jsonPath("$.data.usedStorageBytes").value(expectedBytes))
                .andExpect(jsonPath("$.data.usedStorageFormatted").value("30 B"))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.successRate").value(50.0))
                .andExpect(jsonPath("$.data.activeSchedules").value(0))
                .andExpect(jsonPath("$.data.lastBackupDate").isNotEmpty())
                .andExpect(jsonPath("$.data.nextScheduledBackup").value(nullValue()));

        createSchedule(token, fileId);

        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeSchedules").value(1))
                .andExpect(jsonPath("$.data.nextScheduledBackup").isNotEmpty());
    }

    @Test
    void listsFiltersAndMarksNotificationsRead() throws Exception {
        String token = registerAndLogin(EMAIL);
        String fileId = uploadFile(token, "notifications-ok.txt");
        String failingFileId = uploadFile(token, "notifications-fail.txt");

        FileEntity failingFile = fileRepository.findById(UUID.fromString(failingFileId))
                .orElseThrow();
        failingFile.setSha256Hash("0".repeat(64));
        fileRepository.save(failingFile);

        initiateBackup(token, fileId);
        initiateBackup(token, failingFileId);
        waitForBackupStatus(UUID.fromString(fileId), BackupStatus.SUCCESS);
        waitForBackupStatus(UUID.fromString(failingFileId), BackupStatus.FAILED);
        waitForNotificationCount(2);

        MvcResult listResult = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.unreadCount").value(2))
                .andReturn();

        JsonNode content = objectMapper.readTree(
                listResult.getResponse().getContentAsString()).get("data").get("content");
        assertEquals(2, content.size());

        String firstNotificationId = content.get(0).get("id").asText();
        List<String> types = List.of(
                content.get(0).get("type").asText(),
                content.get(1).get("type").asText());
        assertTrue(types.contains(NotificationType.BACKUP_SUCCESS.name()));
        assertTrue(types.contains(NotificationType.BACKUP_FAILED.name()));

        mockMvc.perform(get("/api/notifications")
                        .param("is_read", "false")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));

        mockMvc.perform(get("/api/notifications")
                        .param("is_read", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(patch("/api/notifications/{id}/read", firstNotificationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(firstNotificationId))
                .andExpect(jsonPath("$.data.isRead").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Notification marquée comme lue"));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatedCount").value(1))
                .andExpect(jsonPath("$.message")
                        .value("1 notification marquée comme lue"));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        String otherToken = registerAndLogin("phase7-other@example.com");
        mockMvc.perform(patch("/api/notifications/{id}/read", firstNotificationId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Cette notification appartient à un autre utilisateur"));

        mockMvc.perform(patch("/api/notifications/{id}/read", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notification introuvable"));

        mockMvc.perform(get("/api/notifications")
                        .param("size", "51")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsScheduleNotificationsAndNotifiesWhenSuspended() throws Exception {
        String token = registerAndLogin(EMAIL);
        String fileId = uploadFile(token, "schedule-notifications.txt");
        String scheduleId = createSchedule(token, fileId);

        assertTrue(hasNotification(NotificationType.SCHEDULE_ACTIVATED));

        mockMvc.perform(patch("/api/schedules/{id}/toggle", scheduleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));

        assertTrue(hasNotification(NotificationType.SCHEDULE_DEACTIVATED));

        mockMvc.perform(patch("/api/schedules/{id}/toggle", scheduleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(true));

        BackupScheduleEntity schedule = scheduleRepository
                .findById(UUID.fromString(scheduleId)).orElseThrow();
        schedule.setConsecutiveFailures(2);
        schedule.setActive(true);
        schedule.setNextRunAt(Instant.now().minusSeconds(60));
        scheduleRepository.save(schedule);

        FileEntity file = fileRepository.findById(UUID.fromString(fileId)).orElseThrow();
        file.setSha256Hash("0".repeat(64));
        fileRepository.save(file);

        scheduleService.executeDueSchedules();

        BackupScheduleEntity suspended = scheduleRepository
                .findById(UUID.fromString(scheduleId)).orElseThrow();
        assertEquals(false, suspended.isActive());
        assertEquals(3, suspended.getConsecutiveFailures());

        List<NotificationEntity> notifications = notificationRepository.findAll();
        assertTrue(notifications.stream().anyMatch(notification ->
                notification.getType() == NotificationType.SCHEDULE_DEACTIVATED
                        && notification.getMessage().contains("suspendue")));
        assertTrue(notifications.stream().anyMatch(notification ->
                notification.getType() == NotificationType.BACKUP_FAILED));
    }

    private boolean hasNotification(NotificationType type) {
        return notificationRepository.findAll().stream()
                .anyMatch(notification -> notification.getType() == type);
    }

    private void waitForNotificationCount(long expected) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (notificationRepository.count() >= expected) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Notifications non créées");
    }

    private void initiateBackup(String token, String fileId) throws Exception {
        mockMvc.perform(post("/api/backups/{fileId}", fileId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted());
    }

    private void waitForBackupStatus(UUID fileId, BackupStatus expected) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            BackupEntity backup = backupRepository.findAll().stream()
                    .filter(candidate -> fileId.equals(candidate.getFileId()))
                    .findFirst()
                    .orElse(null);
            if (backup != null && backup.getStatus() == expected) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Sauvegarde non terminée avec le statut " + expected);
    }

    private String createSchedule(String token, String fileId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "fileId", fileId,
                                "frequency", "DAILY",
                                "scheduledTime", "02:00",
                                "timezone", "UTC"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fileName").isNotEmpty())
                .andReturn();
        return readJson(result, "data", "id");
    }

    private String uploadFile(String token, String fileName) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                MediaType.TEXT_PLAIN_VALUE,
                CONTENT.getBytes(StandardCharsets.UTF_8));

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
                        .content(json(Map.of(
                                "email", email,
                                "password", PASSWORD,
                                "fullName", "Phase 7 User"))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(loginResult, "data", "accessToken");
    }

    private String readJson(MvcResult result, String... path) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        for (String segment : path) {
            node = node.get(segment);
        }
        return node.asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private void cleanDatabase() {
        notificationRepository.deleteAll();
        executionRepository.deleteAll();
        scheduleRepository.deleteAll();
        activityLogRepository.deleteAll();
        restoreJobRepository.deleteAll();
        backupRepository.deleteAll();
        fileRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }
}
