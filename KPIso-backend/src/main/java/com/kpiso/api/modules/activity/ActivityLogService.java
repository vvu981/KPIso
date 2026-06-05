package com.kpiso.api.modules.activity;

import com.kpiso.api.modules.activity.dto.ActivityLogResponse;
import com.kpiso.api.modules.house.House;
import com.kpiso.api.modules.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional
    public void log(String message, String actionType, House house, User user) {
        ActivityLog activityLog = ActivityLog.builder()
                .description(message)
                .actionType(actionType)
                .house(house)
                .user(user)
                .build();
        activityLogRepository.save(activityLog);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> getHouseActivity(UUID houseId, Pageable pageable) {
        return activityLogRepository.findByHouseIdOrderByCreatedAtDesc(houseId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Motor de Limpieza Autónoma.
     * Se ejecuta todos los días a las 03:00 AM.
     * Purga definitivamente todos los registros que superen la política de
     * retención de 90 días.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void purgeOldActivityLogs() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
        activityLogRepository.deleteOlderThan(cutoffDate);
    }

    private ActivityLogResponse mapToResponse(ActivityLog log) {
        return ActivityLogResponse.builder()
                .id(log.getId())
                .description(log.getDescription())
                .actionType(log.getActionType())
                .username(log.getUser().getUsername())
                .createdAt(log.getCreatedAt())
                .build();
    }
}