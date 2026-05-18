package com.kpiso.api.modules.activity;

import com.kpiso.api.modules.activity.dto.ActivityLogResponse;
import com.kpiso.api.modules.house.House;
import com.kpiso.api.modules.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional
    public void log(String description, String actionType, House house, User user) {
        ActivityLog log = ActivityLog.builder()
                .description(description)
                .actionType(actionType)
                .house(house)
                .user(user)
                .build();
        activityLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getHouseLogs(UUID houseId) {
        return activityLogRepository.findByHouseIdOrderByCreatedAtDesc(houseId).stream()
                .map(log -> ActivityLogResponse.builder()
                        .id(log.getId())
                        .description(log.getDescription())
                        .actionType(log.getActionType())
                        .username(log.getUser() != null ? log.getUser().getUsername() : "Sistema")
                        .createdAt(log.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}