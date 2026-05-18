package com.kpiso.api.modules.task;

import com.kpiso.api.modules.task.dto.CreateTaskRequest;
import com.kpiso.api.modules.task.dto.TaskResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<?> createTask(@Valid @RequestBody CreateTaskRequest request) {
        try {
            List<TaskResponse> tasks = taskService.createTask(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(tasks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error en el motor de tareas: " + e.getMessage());
        }
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<?> updateTask(@PathVariable UUID taskId, @Valid @RequestBody CreateTaskRequest request, @RequestParam UUID userId) {
        try {
            TaskResponse task = taskService.updateTask(taskId, request, userId);
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/house/{houseId}")
    public ResponseEntity<List<TaskResponse>> getHouseTasks(@PathVariable UUID houseId) {
        List<TaskResponse> tasks = taskService.getHouseTasks(houseId);
        return ResponseEntity.ok(tasks);
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID taskId, @RequestParam TaskStatus status) {
        try {
            TaskResponse task = taskService.updateTaskStatus(taskId, status);
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PatchMapping("/{taskId}/due-date")
    public ResponseEntity<?> updateDueDate(@PathVariable UUID taskId, @RequestParam String dueDate, @RequestParam UUID userId) {
        try {
            LocalDateTime newDate = LocalDateTime.parse(dueDate);
            TaskResponse task = taskService.updateTaskDueDate(taskId, newDate, userId);
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable UUID taskId, @RequestParam UUID userId) {
        try {
            taskService.softDeleteTask(taskId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}