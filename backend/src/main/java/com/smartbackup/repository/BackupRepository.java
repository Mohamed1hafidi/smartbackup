package com.smartbackup.repository;

import com.smartbackup.entity.BackupEntity;
import com.smartbackup.entity.BackupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface BackupRepository extends JpaRepository<BackupEntity, UUID>,
        JpaSpecificationExecutor<BackupEntity> {

    boolean existsByFileIdAndStatusIn(UUID fileId, Collection<BackupStatus> statuses);

    long countByFileIdAndStatus(UUID fileId, BackupStatus status);

    long countByUserId(UUID userId);

    long countByUserIdAndStatus(UUID userId, BackupStatus status);

    Optional<BackupEntity> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<BackupEntity> findFirstByFileIdAndStatusOrderByVersionNumberAsc(
            UUID fileId, BackupStatus status);

    @Query("select coalesce(max(b.versionNumber), 0) from BackupEntity b where b.fileId = :fileId")
    int findMaxVersionNumberByFileId(@Param("fileId") UUID fileId);
}
