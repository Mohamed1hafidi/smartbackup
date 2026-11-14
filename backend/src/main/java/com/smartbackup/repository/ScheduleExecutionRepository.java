package com.smartbackup.repository;

import com.smartbackup.entity.ScheduleExecutionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScheduleExecutionRepository extends JpaRepository<ScheduleExecutionEntity, UUID> {

    Page<ScheduleExecutionEntity> findByScheduleId(UUID scheduleId, Pageable pageable);

    List<ScheduleExecutionEntity> findByScheduleIdOrderByStartedAtDesc(UUID scheduleId);
}
