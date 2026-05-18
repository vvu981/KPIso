package com.kpiso.api.modules.activity;

import com.kpiso.api.modules.activity.dto.ActivityLogResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/activities")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    public ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping("/house/{houseId}")
    public ResponseEntity<List<ActivityLogResponse>> getHouseLogs(@PathVariable UUID houseId) {
        List<ActivityLogResponse> logs = activityLogService.getHouseLogs(houseId);
        return ResponseEntity.ok(logs);
    }
}