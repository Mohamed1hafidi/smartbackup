package com.smartbackup.controller;

import com.smartbackup.dto.response.ApiResponse;
import com.smartbackup.dto.response.DownloadResponse;
import com.smartbackup.dto.response.FileResponse;
import com.smartbackup.dto.response.FileSummaryResponse;
import com.smartbackup.dto.response.PageResponse;
import com.smartbackup.security.UserPrincipal;
import com.smartbackup.service.FileService;
import com.smartbackup.util.PageableUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Fichiers", description = "Gestion des fichiers de l'utilisateur")
public class FileController {

    private final FileService fileService;

    @PostMapping
    @Operation(summary = "Uploader un fichier")
    public ResponseEntity<ApiResponse<FileResponse>> upload(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file
    ) {
        FileResponse response = fileService.upload(principal.getId(), file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Fichier uploadé avec succès"));
    }

    @GetMapping
    @Operation(summary = "Lister les fichiers de l'utilisateur")
    public ApiResponse<PageResponse<FileSummaryResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PageableUtil.create(page, size, sort, direction);
        return ApiResponse.success(fileService.list(principal.getId(), search, pageable), null);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer les métadonnées d'un fichier")
    public ApiResponse<FileResponse> get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return ApiResponse.success(fileService.get(principal.getId(), id), null);
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Générer une URL de téléchargement signée")
    public ApiResponse<DownloadResponse> download(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return ApiResponse.success(fileService.download(principal.getId(), id), null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer logiquement un fichier")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        fileService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
