package com.smartbackup.service;

import com.smartbackup.dto.response.NotificationListResponse;
import com.smartbackup.dto.response.NotificationReadResponse;
import com.smartbackup.dto.response.NotificationResponse;
import com.smartbackup.entity.NotificationEntity;
import com.smartbackup.entity.NotificationType;
import com.smartbackup.exception.ResourceNotFoundException;
import com.smartbackup.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public NotificationListResponse list(UUID userId, Boolean isRead, Pageable pageable) {
        Page<NotificationEntity> page = isRead == null
                ? notificationRepository.findByUserId(userId, pageable)
                : notificationRepository.findByUserIdAndRead(userId, isRead, pageable);

        long unreadCount = notificationRepository.countByUserIdAndReadFalse(userId);

        return new NotificationListResponse(
                page.getContent().stream().map(NotificationResponse::from).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                unreadCount
        );
    }

    @Transactional
    public NotificationReadResponse markRead(UUID userId, UUID notificationId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable"));

        if (!notification.getUserId().equals(userId)) {
            throw new AccessDeniedException(
                    "Cette notification appartient à un autre utilisateur");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }

        return new NotificationReadResponse(notification.getId(), notification.isRead());
    }

    @Transactional
    public long markAllRead(UUID userId) {
        return notificationRepository.markAllRead(userId);
    }

    public void create(UUID userId, UUID backupId, NotificationType type, String message) {
        NotificationEntity notification = NotificationEntity.builder()
                .userId(userId)
                .backupId(backupId)
                .type(type)
                .message(message)
                .read(false)
                .build();

        notificationRepository.save(notification);
        log.debug("Notification {} créée pour l'utilisateur {}", type, userId);

        emailService.send(userId, type, message);
    }
}
