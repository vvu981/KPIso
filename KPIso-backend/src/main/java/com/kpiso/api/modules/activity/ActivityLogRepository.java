package com.kpiso.api.modules.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

    // Paginación delegada a nivel de motor de base de datos para no cargar arrays
    // masivos en memoria
    Page<ActivityLog> findByHouseIdOrderByCreatedAtDesc(UUID houseId, Pageable pageable);

    // Borrado masivo por fecha (altamente eficiente frente a iteraciones
    // individuales)
    @Modifying
    @Query("DELETE FROM ActivityLog a WHERE a.createdAt < :cutoffDate")
    void deleteOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}