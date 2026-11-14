package com.smartbackup.repository;

import com.smartbackup.entity.RestoreJobEntity;
import com.smartbackup.entity.RestoreStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface RestoreJobRepository extends JpaRepository<RestoreJobEntity, UUID> {

    boolean existsByFileIdAndStatusIn(UUID fileId, Collection<RestoreStatus> statuses);
}
