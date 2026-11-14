package com.smartbackup.repository;

import com.smartbackup.entity.ActivityLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ActivityLogRepository extends JpaRepository<ActivityLogEntity, UUID> {

    long countByAction(String action);
}
