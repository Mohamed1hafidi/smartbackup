package com.smartbackup;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileFlowIntegrationTest {

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

    @MockBean
    private StorageService storageService;

    @BeforeEach
    void cleanDatabase() {
        fileRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void uploadListDownloadAndDeleteFlow() throws Exception {
        String accessToken = registerAndLogin("files@example.com");

        when(storageService.generateDownloadUrl(anyString()))
                .thenReturn(new StorageService.PresignedUrl(
                        "https://example.com/download", Instant.now().plusSeconds(900)));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rapportannuel.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "contenu du rapport".getBytes(StandardCharsets.UTF_8));

        MvcResult uploadResult = mockMvc.perform(multipart("/api/files")
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Fichier uploadé avec succès"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.originalName").value("rapportannuel.txt"))
                .andExpect(jsonPath("$.data.mimeType").value("text/plain"))
                .andExpect(jsonPath("$.data.fileSize").value(file.getSize()))
                .andExpect(jsonPath("$.data.sha256Hash").isNotEmpty())
                .andExpect(jsonPath("$.data.storedKey").isNotEmpty())
                .andReturn();

        verify(storageService).upload(anyString(), anyString(), anyLong(), any());

        String fileId = readJson(uploadResult, "data", "id");
        String storedKey = readJson(uploadResult, "data", "storedKey");

        mockMvc.perform(get("/api/files")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(fileId))
                .andExpect(jsonPath("$.data.content[0].originalName").value("rapportannuel.txt"));

        mockMvc.perform(get("/api/files")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("search", "rapport"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/files/{id}", fileId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storedKey").value(storedKey));

        mockMvc.perform(get("/api/files/{id}/download", fileId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.downloadUrl").value("https://example.com/download"))
                .andExpect(jsonPath("$.data.fileName").value("rapportannuel.txt"))
                .andExpect(jsonPath("$.data.expiresAt").isNotEmpty());

        mockMvc.perform(delete("/api/files/{id}", fileId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/files/{id}", fileId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsUnsupportedFileType() throws Exception {
        String accessToken = registerAndLogin("files@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.exe",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                new byte[]{0x4D, 0x5A, 0x00, 0x01, 0x02, 0x03});

        mockMvc.perform(multipart("/api/files")
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Type de fichier non autorisé : application/x-msdownload"));
    }

    @Test
    void rejectsMissingFile() throws Exception {
        String accessToken = registerAndLogin("files@example.com");

        mockMvc.perform(multipart("/api/files")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAccessToAnotherUsersFile() throws Exception {
        String ownerToken = registerAndLogin("owner@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "prive.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "données privées".getBytes(StandardCharsets.UTF_8));

        MvcResult uploadResult = mockMvc.perform(multipart("/api/files")
                        .file(file)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isCreated())
                .andReturn();

        String fileId = readJson(uploadResult, "data", "id");
        String intruderToken = registerAndLogin("intruder@example.com");

        mockMvc.perform(get("/api/files/{id}", fileId)
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Ce fichier appartient à un autre utilisateur"));
    }

    @Test
    void rejectsUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/files"))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Secure@123",
                                "fullName", "File User"))))
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
