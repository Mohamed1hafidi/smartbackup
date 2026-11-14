package com.smartbackup.controller;

import com.smartbackup.dto.response.ApiResponse;
import com.smartbackup.dto.response.DownloadResponse;
import com.smartbackup.dto.response.RestoreResponse;
import com.smartbackup.security.UserPrincipal;
import com.smartbackup.service.RestoreService;
import com.smartbackup.util.ClientIpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/backups")
@RequiredArgsConstructor
@Tag(name = "Restauration", description = "Téléchargement et restauration des sauvegardes")
public class RestoreController {

    private final RestoreService restoreService;

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restaurer une sauvegarde")
    public ResponseEntity<ApiResponse<RestoreResponse>> restore(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        RestoreResponse response = restoreService.initiateRestore(
                principal.getId(), id, ClientIpUtil.resolve(request));
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, "Restauration démarrée"));
    }

    @GetMapping("/restores/{jobId}")
    @Operation(summary = "Consulter l'état d'une restauration")
    public ApiResponse<RestoreResponse> status(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId
    ) {
        return ApiResponse.success(restoreService.getRestore(principal.getId(), jobId), null);
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Télécharger une sauvegarde via une URL temporaire")
    public ApiResponse<DownloadResponse> download(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return ApiResponse.success(
                restoreService.getBackupDownloadUrl(principal.getId(), id), null);
    }
}
