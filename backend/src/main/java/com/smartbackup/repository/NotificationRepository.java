package com.smartbackup.repository;

import com.smartbackup.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    Page<NotificationEntity> findByUserId(UUID userId, Pageable pageable);

    Page<NotificationEntity> findByUserIdAndRead(UUID userId, boolean read, Pageable pageable);

    long countByUserIdAndReadFalse(UUID userId);

    Optional<NotificationEntity> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("update NotificationEntity n set n.read = true where n.userId = :userId and n.read = false")
    int markAllRead(@Param("userId") UUID userId);
}
