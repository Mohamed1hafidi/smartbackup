package com.smartbackup.controller;

import com.smartbackup.dto.request.CreateScheduleRequest;
import com.smartbackup.dto.request.UpdateScheduleRequest;
import com.smartbackup.dto.response.ApiResponse;
import com.smartbackup.dto.response.PageResponse;
import com.smartbackup.dto.response.ScheduleExecutionResponse;
import com.smartbackup.dto.response.ScheduleListResponse;
import com.smartbackup.dto.response.ScheduleResponse;
import com.smartbackup.security.UserPrincipal;
import com.smartbackup.service.ScheduleService;
import com.smartbackup.util.ClientIpUtil;
import com.smartbackup.util.PageableUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@Tag(name = "Programmations", description = "Sauvegardes automatiques planifiées")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    @Operation(summary = "Créer une programmation")
    public ResponseEntity<ApiResponse<ScheduleResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateScheduleRequest request,
            HttpServletRequest httpRequest
    ) {
        ScheduleResponse response = scheduleService.create(
                principal.getId(), request, ClientIpUtil.resolve(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Programmation créée"));
    }

    @GetMapping
    @Operation(summary = "Lister les programmations de l'utilisateur")
    public ScheduleListResponse list(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        var schedules = scheduleService.list(principal.getId());
        return new ScheduleListResponse(schedules, schedules.size());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une programmation")
    public ApiResponse<ScheduleResponse> get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return ApiResponse.success(scheduleService.get(principal.getId(), id), null);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une programmation")
    public ApiResponse<ScheduleResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateScheduleRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(
                scheduleService.update(
                        principal.getId(), id, request,
                        ClientIpUtil.resolve(httpRequest)),
                "Programmation mise à jour");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une programmation")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        scheduleService.delete(
                principal.getId(), id, ClientIpUtil.resolve(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Activer ou désactiver une programmation")
    public ApiResponse<ScheduleResponse> toggle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        ScheduleResponse response = scheduleService.toggle(
                principal.getId(), id, ClientIpUtil.resolve(httpRequest));
        String message = response.isActive()
                ? "Programmation activée"
                : "Programmation désactivée";
        return ApiResponse.success(response, message);
    }

    @GetMapping("/{id}/executions")
    @Operation(summary = "Consulter le journal d'exécution d'une programmation")
    public ApiResponse<PageResponse<ScheduleExecutionResponse>> executions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageableUtil.create(page, size, "createdAt", "desc");
        return ApiResponse.success(
                scheduleService.listExecutions(principal.getId(), id, pageable), null);
    }
}
