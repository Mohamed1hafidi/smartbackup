package com.smartbackup.dto.response;

import java.util.UUID;

public record NotificationReadResponse(UUID id, boolean isRead) {
}
