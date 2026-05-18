package com.kpiso.api.modules.task.dto;

import com.kpiso.api.modules.task.RotationType;
import com.kpiso.api.modules.task.TaskStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {
    private UUID id;
    private String title;
    private String description;
    private Integer points;
    private TaskStatus status;
    private RotationType rotationType;
    private LocalDateTime dueDate;
    private AssignedUserResponse assignedTo;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignedUserResponse {
        private UUID id;
        private String username;
        private String color; // Propiedad añadida para pintar los bordes en el calendario
    }
}