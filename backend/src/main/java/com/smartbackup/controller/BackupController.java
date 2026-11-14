package com.smartbackup.controller;

import com.smartbackup.dto.response.ApiResponse;
import com.smartbackup.dto.response.BackupResponse;
import com.smartbackup.dto.response.BackupSummaryResponse;
import com.smartbackup.dto.response.PageResponse;
import com.smartbackup.entity.BackupStatus;
import com.smartbackup.security.UserPrincipal;
import com.smartbackup.service.BackupService;
import com.smartbackup.util.PageableUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/backups")
@RequiredArgsConstructor
@Tag(name = "Sauvegardes", description = "Sauvegardes manuelles et historique des versions")
public class BackupController {

    private final BackupService backupService;

    @PostMapping("/{fileId}")
    @Operation(summary = "Déclencher une sauvegarde manuelle")
    public ResponseEntity<ApiResponse<BackupResponse>> createManualBackup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fileId
    ) {
        BackupResponse response = backupService.initiateManualBackup(principal.getId(), fileId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, "Sauvegarde démarrée"));
    }

    @GetMapping
    @Operation(summary = "Consulter l'historique des sauvegardes")
    public ApiResponse<PageResponse<BackupSummaryResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) BackupStatus status,
            @RequestParam(required = false) UUID fileId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo
    ) {
        Pageable pageable = PageableUtil.create(page, size, "createdAt", "desc");
        return ApiResponse.success(
                backupService.list(principal.getId(), status, fileId, dateFrom, dateTo, pageable),
                null);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter le détail d'une sauvegarde")
    public ApiResponse<BackupResponse> get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return ApiResponse.success(backupService.get(principal.getId(), id), null);
    }
}
