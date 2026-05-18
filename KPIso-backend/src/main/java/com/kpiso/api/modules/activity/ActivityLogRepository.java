package com.kpiso.api.modules.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {
    List<ActivityLog> findByHouseIdOrderByCreatedAtDesc(UUID houseId);
}