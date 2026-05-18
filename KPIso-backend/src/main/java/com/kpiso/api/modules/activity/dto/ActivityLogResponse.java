package com.kpiso.api.modules.activity.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLogResponse {
    private UUID id;
    private String description;
    private String actionType;
    private String username;
    private LocalDateTime createdAt;
}