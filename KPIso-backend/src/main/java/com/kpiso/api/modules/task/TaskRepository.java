package com.kpiso.api.modules.task;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByHouseIdAndDeletedAtIsNull(UUID houseId);

    // Filtro para extraer los puntos ganados de forma eficiente
    List<Task> findByHouseIdAndStatusAndDeletedAtIsNull(UUID houseId, TaskStatus status);
}