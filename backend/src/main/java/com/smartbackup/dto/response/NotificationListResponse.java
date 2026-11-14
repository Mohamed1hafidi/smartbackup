package com.smartbackup.dto.response;

import java.util.List;

public record NotificationListResponse(
        List<NotificationResponse> content,
        long totalElements,
        int totalPages,
        long unreadCount
) {
}
