package com.smartbackup.repository;

import com.smartbackup.entity.BackupScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BackupScheduleRepository extends JpaRepository<BackupScheduleEntity, UUID> {

    List<BackupScheduleEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<BackupScheduleEntity> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndActiveTrue(UUID userId);

    Optional<BackupScheduleEntity>
    findFirstByUserIdAndActiveTrueAndNextRunAtIsNotNullOrderByNextRunAtAsc(UUID userId);

    boolean existsByUserIdAndFileIdAndActiveTrue(UUID userId, UUID fileId);

    boolean existsByUserIdAndFileIdAndActiveTrueAndIdNot(UUID userId, UUID fileId, UUID id);

    List<BackupScheduleEntity> findByActiveTrueAndNextRunAtLessThanEqualOrderByNextRunAtAsc(
            Instant now);
}
