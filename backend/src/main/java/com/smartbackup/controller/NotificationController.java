package com.smartbackup.controller;

import com.smartbackup.dto.response.ApiResponse;
import com.smartbackup.dto.response.MarkAllReadResponse;
import com.smartbackup.dto.response.NotificationListResponse;
import com.smartbackup.dto.response.NotificationReadResponse;
import com.smartbackup.security.UserPrincipal;
import com.smartbackup.service.NotificationService;
import com.smartbackup.util.PageableUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notifications in-app de l'utilisateur")
public class NotificationController {

    private static final int MAX_PAGE_SIZE = 50;

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Lister les notifications de l'utilisateur")
    public ApiResponse<NotificationListResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(name = "is_read", required = false) Boolean isRead
    ) {
        Pageable pageable = PageableUtil.create(
                page, size, "createdAt", "desc", MAX_PAGE_SIZE);
        return ApiResponse.success(
                notificationService.list(principal.getId(), isRead, pageable), null);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marquer une notification comme lue")
    public ApiResponse<NotificationReadResponse> markRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return ApiResponse.success(
                notificationService.markRead(principal.getId(), id),
                "Notification marquée comme lue");
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Marquer toutes les notifications comme lues")
    public ApiResponse<MarkAllReadResponse> markAllRead(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        long updatedCount = notificationService.markAllRead(principal.getId());
        String message = updatedCount == 1
                ? "1 notification marquée comme lue"
                : updatedCount + " notifications marquées comme lues";
        return ApiResponse.success(new MarkAllReadResponse(updatedCount), message);
    }
}
