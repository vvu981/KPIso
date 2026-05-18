package com.kpiso.api.modules.task;

import com.kpiso.api.modules.task.dto.CreateTaskRequest;
import com.kpiso.api.modules.task.dto.TaskResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/house/{houseId}")
    public ResponseEntity<List<TaskResponse>> getTasksByHouse(@PathVariable UUID houseId) {
        return ResponseEntity.ok(taskService.getTasksByHouse(houseId));
    }

    @PostMapping
    public ResponseEntity<List<TaskResponse>> createTask(@RequestBody CreateTaskRequest request) {
        return ResponseEntity.ok(taskService.createTask(request));
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<?> toggleTaskStatus(
            @PathVariable UUID taskId,
            @RequestParam String status,
            @RequestParam UUID userId) {
        try {
            taskService.toggleTaskStatus(taskId, status, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{taskId}/due-date")
    public ResponseEntity<TaskResponse> updateTaskDueDate(
            @PathVariable UUID taskId,
            @RequestParam String dueDate,
            @RequestParam UUID userId) {
        java.time.LocalDateTime date = java.time.LocalDateTime.parse(dueDate);
        return ResponseEntity.ok(taskService.updateTaskDueDate(taskId, date, userId));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable UUID taskId,
            @RequestBody CreateTaskRequest request,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(taskService.updateTask(taskId, request, userId));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable UUID taskId, @RequestParam UUID userId) {
        try {
            taskService.softDeleteTask(taskId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}