package com.smartbackup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbackup.entity.BackupEntity;
import com.smartbackup.entity.BackupScheduleEntity;
import com.smartbackup.entity.BackupStatus;
import com.smartbackup.entity.BackupType;
import com.smartbackup.entity.FileEntity;
import com.smartbackup.entity.ScheduleExecutionEntity;
import com.smartbackup.entity.ScheduleExecutionStatus;
import com.smartbackup.repository.ActivityLogRepository;
import com.smartbackup.repository.BackupRepository;
import com.smartbackup.repository.BackupScheduleRepository;
import com.smartbackup.repository.FileRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScheduleFlowIntegrationTest {

    private static final String FILE_NAME = "planifie.txt";
    private static final String FILE_CONTENT = "contenu planifié";

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
    private ScheduleService scheduleService;

    @MockBean
    private StorageService storageService;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        doAnswer(invocation -> {
            Path destination = invocation.getArgument(1);
            Files.writeString(destination, FILE_CONTENT);
            return null;
        }).when(storageService).download(anyString(), any(Path.class));
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void createsListsUpdatesTogglesAndDeletesSchedule() throws Exception {
        String token = registerAndLogin("schedule-crud@example.com");
        String fileId = uploadFile(token);

        MvcResult createResult = mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "fileId", fileId,
                                "frequency", "DAILY",
                                "scheduledTime", "02:00",
                                "timezone", "UTC"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Programmation créée"))
                .andExpect(jsonPath("$.data.fileId").value(fileId))
                .andExpect(jsonPath("$.data.fileName").value(FILE_NAME))
                .andExpect(jsonPath("$.data.frequency").value("DAILY"))
                .andExpect(jsonPath("$.data.scheduledTime").value("02:00"))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.nextRunAt").isNotEmpty())
                .andReturn();

        String scheduleId = readJson(createResult, "data", "id");

        mockMvc.perform(get("/api/schedules")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].id").value(scheduleId));

        mockMvc.perform(get("/api/schedules/{id}", scheduleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());

        mockMvc.perform(put("/api/schedules/{id}", scheduleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "frequency", "WEEKLY",
                                "scheduledTime", "03:30",
                                "dayOfWeek", "MONDAY",
                                "timezone", "Europe/Paris"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Programmation mise à jour"))
                .andExpect(jsonPath("$.data.frequency").value("WEEKLY"))
                .andExpect(jsonPath("$.data.scheduledTime").value("03:30"))
                .andExpect(jsonPath("$.data.dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.data.nextRunAt").isNotEmpty());

        mockMvc.perform(patch("/api/schedules/{id}/toggle", scheduleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Programmation désactivée"))
                .andExpect(jsonPath("$.data.isActive").value(false))
                .andExpect(jsonPath("$.data.nextRunAt").doesNotExist());

        mockMvc.perform(patch("/api/schedules/{id}/toggle", scheduleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Programmation activée"))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.nextRunAt").isNotEmpty());

        mockMvc.perform(delete("/api/schedules/{id}", scheduleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/schedules/{id}", scheduleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Programmation introuvable"));
    }

    @Test
    void rejectsDuplicateActiveScheduleForSameFile() throws Exception {
        String token = registerAndLogin("schedule-duplicate@example.com");
        String fileId = uploadFile(token);
        createSchedule(token, fileId, "DAILY", "02:00", null, "UTC");

        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "fileId", fileId,
                                "frequency", "DAILY",
                                "scheduledTime", "03:00",
                                "timezone", "UTC"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Une programmation active existe déjà pour ce fichier"));
    }

    @Test
    void rejectsCrossUserSchedulesAndUnauthenticatedAccess() throws Exception {
        String ownerToken = registerAndLogin("schedule-owner@example.com");
        String fileId = uploadFile(ownerToken);
        String scheduleId = createSchedule(
                ownerToken, fileId, "DAILY", "02:00", null, "UTC");

        String intruderToken = registerAndLogin("schedule-intruder@example.com");

        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "fileId", fileId,
                                "frequency", "DAILY",
                                "scheduledTime", "02:00",
                                "timezone", "UTC"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Ce fichier appartient à un autre utilisateur"));

        mockMvc.perform(get("/api/schedules/{id}", scheduleId)
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Cette programmation appartient à un autre utilisateur"));

        mockMvc.perform(get("/api/schedules"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validatesScheduleInputs() throws Exception {
        String token = registerAndLogin("schedule-validation@example.com");
        String fileId = uploadFile(token);

        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "fileId", fileId,
                                "frequency", "WEEKLY",
                                "scheduledTime", "02:00",
                                "timezone", "UTC"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("dayOfWeek est requis pour une fréquence WEEKLY"));

        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "fileId", fileId,
                                "frequency", "DAILY",
                                "scheduledTime", "25:00",
                                "timezone", "UTC"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "fileId", fileId,
                                "frequency", "DAILY",
                                "scheduledTime", "02:00",
                                "timezone", "Mars/Olympus"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Fuseau horaire invalide"));

        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "fileId", fileId,
                                "frequency", "HOURLY",
                                "scheduledTime", "02:00",
                                "timezone", "UTC"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enforcesFiveActiveSchedulesLimit() throws Exception {
        String token = registerAndLogin("schedule-limit@example.com");

        for (int index = 0; index < 5; index++) {
            String fileId = uploadFile(token);
            createSchedule(token, fileId, "DAILY", "02:00", null, "UTC");
        }

        String extraFileId = uploadFile(token);
        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "fileId", extraFileId,
                                "frequency", "DAILY",
                                "scheduledTime", "02:00",
                                "timezone", "UTC"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Limite de 5 programmations actives atteinte"));
    }

    @Test
    void executesDueScheduleAndJournalsSuccess() throws Exception {
        String token = registerAndLogin("schedule-execute@example.com");
        String fileId = uploadFile(token);
        String scheduleId = createSchedule(
                token, fileId, "DAILY", "02:00", null, "UTC");

        BackupScheduleEntity schedule = scheduleRepository
                .findById(UUID.fromString(scheduleId)).orElseThrow();
        schedule.setNextRunAt(Instant.now().minusSeconds(60));
        scheduleRepository.save(schedule);

        int executed = scheduleService.executeDueSchedules();
        assertEquals(1, executed);

        BackupScheduleEntity updated = scheduleRepository
                .findById(UUID.fromString(scheduleId)).orElseThrow();
        assertNotNull(updated.getLastRunAt());
        assertNotNull(updated.getNextRunAt());
        assertTrue(updated.getNextRunAt().isAfter(Instant.now().minusSeconds(5)));
        assertTrue(updated.isActive());
        assertEquals(0, updated.getConsecutiveFailures());

        List<BackupEntity> backups = backupRepository.findAll();
        assertEquals(1, backups.size());
        assertEquals(BackupType.SCHEDULED, backups.get(0).getBackupType());
        assertEquals(BackupStatus.SUCCESS, backups.get(0).getStatus());

        List<ScheduleExecutionEntity> executions = executionRepository
                .findByScheduleIdOrderByStartedAtDesc(UUID.fromString(scheduleId));
        assertEquals(1, executions.size());
        assertEquals(ScheduleExecutionStatus.SUCCESS, executions.get(0).getStatus());
        assertNotNull(executions.get(0).getBackupId());
        assertNotNull(executions.get(0).getDurationMs());
        assertEquals(1, activityLogRepository.countByAction("SCHEDULE_EXECUTED"));

        mockMvc.perform(get("/api/schedules/{id}/executions", scheduleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("SUCCESS"));
    }

    @Test
    void suspendsScheduleAfterThreeConsecutiveFailures() throws Exception {
        String token = registerAndLogin("schedule-fail@example.com");
        String fileId = uploadFile(token);
        String scheduleId = createSchedule(
                token, fileId, "DAILY", "02:00", null, "UTC");

        BackupScheduleEntity schedule = scheduleRepository
                .findById(UUID.fromString(scheduleId)).orElseThrow();
        schedule.setConsecutiveFailures(2);
        schedule.setNextRunAt(Instant.now().minusSeconds(60));
        scheduleRepository.save(schedule);

        doThrow(new com.smartbackup.exception.StorageException("Stockage indisponible"))
                .when(storageService).download(anyString(), any(Path.class));

        int executed = scheduleService.executeDueSchedules();
        assertEquals(1, executed);

        BackupScheduleEntity updated = scheduleRepository
                .findById(UUID.fromString(scheduleId)).orElseThrow();
        assertFalse(updated.isActive());
        assertNull(updated.getNextRunAt());
        assertEquals(3, updated.getConsecutiveFailures());

        List<ScheduleExecutionEntity> executions = executionRepository
                .findByScheduleIdOrderByStartedAtDesc(UUID.fromString(scheduleId));
        assertEquals(1, executions.size());
        assertEquals(ScheduleExecutionStatus.FAILED, executions.get(0).getStatus());
        assertEquals("Stockage indisponible", executions.get(0).getErrorMessage());
        assertEquals(1, activityLogRepository.countByAction("SCHEDULE_EXECUTION_FAILED"));
        assertEquals(1, activityLogRepository.countByAction("SCHEDULE_AUTO_SUSPENDED"));

        mockMvc.perform(patch("/api/schedules/{id}/toggle", scheduleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.nextRunAt").isNotEmpty());

        BackupScheduleEntity reactivated = scheduleRepository
                .findById(UUID.fromString(scheduleId)).orElseThrow();
        assertEquals(0, reactivated.getConsecutiveFailures());
    }

    private String createSchedule(
            String token,
            String fileId,
            String frequency,
            String scheduledTime,
            String dayOfWeek,
            String timezone
    ) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("fileId", fileId);
        body.put("frequency", frequency);
        body.put("scheduledTime", scheduledTime);
        body.put("timezone", timezone);
        if (dayOfWeek != null) {
            body.put("dayOfWeek", dayOfWeek);
        }

        MvcResult result = mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result, "data", "id");
    }

    private String uploadFile(String token) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                FILE_NAME,
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
                        .content(json(Map.of(
                                "email", email,
                                "password", "Secure@123",
                                "fullName", "Schedule User"))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
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

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private void cleanDatabase() {
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
