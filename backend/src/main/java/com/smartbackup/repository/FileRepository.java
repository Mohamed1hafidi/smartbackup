package com.smartbackup.repository;

import com.smartbackup.entity.FileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<FileEntity, UUID> {

    Page<FileEntity> findByUserIdAndDeletedFalse(UUID userId, Pageable pageable);

    Page<FileEntity> findByUserIdAndDeletedFalseAndOriginalNameContainingIgnoreCase(
            UUID userId, String originalName, Pageable pageable);

    Optional<FileEntity> findByIdAndDeletedFalse(UUID id);

    long countByUserIdAndDeletedFalse(UUID userId);

    @Query("""
            select coalesce(sum(f.fileSize), 0)
            from FileEntity f
            where f.userId = :userId and f.deleted = false
            """)
    long sumActiveFileSizeByUserId(@Param("userId") UUID userId);
}
