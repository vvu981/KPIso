package com.kpiso.api.modules.activity;

import com.kpiso.api.modules.activity.dto.ActivityLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/activity")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    public ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    // Adaptado para recibir los parámetros de paginación y emitir un Pageable
    @GetMapping("/house/{houseId}")
    public ResponseEntity<Page<ActivityLogResponse>> getHouseActivity(
            @PathVariable UUID houseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<ActivityLogResponse> activityPage = activityLogService.getHouseActivity(houseId, pageRequest);
        return ResponseEntity.ok(activityPage);
    }
}