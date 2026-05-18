package com.kpiso.api.modules.task;

import com.kpiso.api.modules.house.House;
import com.kpiso.api.modules.house.HouseRepository;
import com.kpiso.api.modules.house.HouseMember;
import com.kpiso.api.modules.house.HouseMemberRepository;
import com.kpiso.api.modules.task.dto.CreateTaskRequest;
import com.kpiso.api.modules.task.dto.TaskResponse;
import com.kpiso.api.modules.user.User;
import com.kpiso.api.modules.user.UserRepository;
import com.kpiso.api.modules.activity.ActivityLogService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final HouseRepository houseRepository;
    private final UserRepository userRepository;
    private final HouseMemberRepository houseMemberRepository;
    private final ActivityLogService activityLogService;

    public TaskService(TaskRepository taskRepository, HouseRepository houseRepository,
                       UserRepository userRepository, HouseMemberRepository houseMemberRepository,
                       ActivityLogService activityLogService) {
        this.taskRepository = taskRepository;
        this.houseRepository = houseRepository;
        this.userRepository = userRepository;
        this.houseMemberRepository = houseMemberRepository;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public List<TaskResponse> createTask(CreateTaskRequest request) {
        House house = houseRepository.findById(request.getHouseId())
                .orElseThrow(() -> new IllegalArgumentException("La casa especificada no existe"));

        List<Task> createdTasks = new ArrayList<>();
        LocalDateTime currentDate = request.getStartDate() != null ? request.getStartDate() : LocalDateTime.now();

        if (request.getRotationType() == RotationType.FIXED) {
            if (request.getAssignedToId() == null) {
                throw new IllegalArgumentException("Se debe asignar un usuario para tareas de tipo fijo");
            }
            User assignee = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new IllegalArgumentException("El usuario asignado no existe"));

            Task task = Task.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .points(request.getPoints())
                    .status(TaskStatus.PENDING)
                    .rotationType(RotationType.FIXED)
                    .dueDate(currentDate)
                    .house(house)
                    .assignedTo(assignee)
                    .build();

            createdTasks.add(taskRepository.save(task));

        } else {
            List<User> participants = new ArrayList<>();
            if (request.getParticipantIds() != null && !request.getParticipantIds().isEmpty()) {
                for (UUID userId : request.getParticipantIds()) {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
                    if (!houseMemberRepository.existsByHouseIdAndUserId(request.getHouseId(), userId)) {
                        throw new IllegalArgumentException("El usuario no pertenece a esta casa");
                    }
                    participants.add(user);
                }
            } else {
                List<HouseMember> members = houseMemberRepository.findByHouseId(request.getHouseId());
                for (HouseMember member : members) {
                    participants.add(member.getUser());
                }
            }

            if (participants.isEmpty()) {
                throw new IllegalArgumentException("No se pueden crear tareas rotativas sin participantes");
            }

            int totalOccurrences = request.getOccurrencesToProject() != null ? request.getOccurrencesToProject() : participants.size();
            int participantIndex = 0;

            for (int i = 0; i < totalOccurrences; i++) {
                User assignee = participants.get(participantIndex);
                currentDate = calculateNextDueDate(currentDate, request.getRotationType(), request.getSpecificDays(), i == 0);

                Task task = Task.builder()
                        .title(request.getTitle() + " (Turno " + (i + 1) + ")")
                        .description(request.getDescription())
                        .points(request.getPoints())
                        .status(TaskStatus.PENDING)
                        .rotationType(request.getRotationType())
                        .dueDate(currentDate)
                        .house(house)
                        .assignedTo(assignee)
                        .build();

                createdTasks.add(taskRepository.save(task));
                participantIndex = (participantIndex + 1) % participants.size();
            }
        }

        return createdTasks.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private LocalDateTime calculateNextDueDate(LocalDateTime current, RotationType type, List<Integer> specificDays, boolean isFirst) {
        if (!isFirst) {
            if (type == RotationType.DAILY) current = current.plusDays(1);
            else if (type == RotationType.WEEKLY && (specificDays == null || specificDays.isEmpty())) current = current.plusWeeks(1);
            else if (type == RotationType.MONTHLY) current = current.plusMonths(1);
        }

        if (type == RotationType.WEEKLY && specificDays != null && !specificDays.isEmpty()) {
            int safetyCheck = 0;
            if (!isFirst) current = current.plusDays(1);
            while (!specificDays.contains(current.getDayOfWeek().getValue()) && safetyCheck < 14) {
                current = current.plusDays(1);
                safetyCheck++;
            }
        }
        return current;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByHouse(UUID houseId) {
        return taskRepository.findByHouseIdAndDeletedAtIsNull(houseId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponse updateTaskStatus(UUID taskId, TaskStatus newStatus) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("La tarea no existe"));
        task.setStatus(newStatus);
        return mapToResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateTaskDueDate(UUID taskId, LocalDateTime newDueDate, UUID requestingUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("La tarea especificada no existe"));

        User actor = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no válido"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String oldDateStr = task.getDueDate() != null ? task.getDueDate().format(formatter) : "Sin fecha";
        String newDateStr = newDueDate.format(formatter);

        task.setDueDate(newDueDate);
        Task updatedTask = taskRepository.save(task);

        String msg = String.format("%s reprogramó el deber '%s': cambió su fecha límite del %s al %s",
                actor.getUsername(), task.getTitle(), oldDateStr, newDateStr);
        activityLogService.log(msg, "UPDATE", task.getHouse(), actor);

        return mapToResponse(updatedTask);
    }

    @Transactional
    public TaskResponse updateTask(UUID taskId, CreateTaskRequest request, UUID requestingUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("La tarea no existe"));

        User actor = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no válido"));

        String oldTitle = task.getTitle();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPoints(request.getPoints());
        task.setRotationType(request.getRotationType());

        // CORRECCIÓN SOLID: Si el formulario de edición envía una fecha específica, se actualiza el día límite de este turno
        if (request.getStartDate() != null) {
            task.setDueDate(request.getStartDate());
        }

        if (request.getAssignedToId() != null) {
            User assignee = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new IllegalArgumentException("El usuario asignado no existe"));
            task.setAssignedTo(assignee);
        } else {
            task.setAssignedTo(null);
        }

        Task saved = taskRepository.save(task);

        String msg = String.format("%s editó los detalles del deber '%s'", actor.getUsername(), oldTitle);
        activityLogService.log(msg, "UPDATE", task.getHouse(), actor);

        return mapToResponse(saved);
    }

    @Transactional
    public void softDeleteTask(UUID taskId, UUID requestingUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("La tarea no existe"));

        User actor = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no válido"));

        task.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task);

        String msg = String.format("%s eliminó de la agenda el deber '%s'", actor.getUsername(), task.getTitle());
        activityLogService.log(msg, "DELETE", task.getHouse(), actor);
    }

    private TaskResponse mapToResponse(Task task) {
        TaskResponse.AssignedUserResponse userDto = null;
        if (task.getAssignedTo() != null) {
            String memberColor = houseMemberRepository.findByHouseIdAndUserId(task.getHouse().getId(), task.getAssignedTo().getId())
                    .map(HouseMember::getColor)
                    .orElse("#6366f1");

            userDto = TaskResponse.AssignedUserResponse.builder()
                    .id(task.getAssignedTo().getId())
                    .username(task.getAssignedTo().getUsername())
                    .color(memberColor)
                    .build();
        }

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .points(task.getPoints())
                .status(task.getStatus())
                .rotationType(task.getRotationType())
                .dueDate(task.getDueDate())
                .assignedTo(userDto)
                .build();
    }

    // MODIFICADO: Sistema unificado de auditoría transparente que elimina el uso de SecurityContextHolder
    @Transactional
    public void toggleTaskStatus(UUID taskId, String targetStatusStr, UUID userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("La tarea especificada no existe"));

        TaskStatus targetStatus = TaskStatus.valueOf(targetStatusStr.toUpperCase());
        task.setStatus(targetStatus);

        // Identificamos directamente al actor que realiza la petición web por su ID
        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario ejecutor no encontrado"));

        if (targetStatus == TaskStatus.COMPLETED) {
            task.setCompletedBy(actor);
            task.setCompletedAt(LocalDateTime.now());

            // Registro inteligente en el diario de transparencia
            String msg;
            User assigned = task.getAssignedTo();
            if (assigned != null && !assigned.getId().equals(userId) && task.getDueDate() != null && task.getCompletedAt().isAfter(task.getDueDate())) {
                msg = String.format("%s rescató el deber vencido '%s' de %s (+%d pts para %s, -%d pts para %s)",
                        actor.getUsername(), task.getTitle(), assigned.getUsername(), task.getPoints(), actor.getUsername(), task.getPoints(), assigned.getUsername());
                activityLogService.log(msg, "UPDATE", task.getHouse(), actor);
            } else {
                msg = String.format("%s completó el deber '%s' (+%d pts)", actor.getUsername(), task.getTitle(), task.getPoints());
                activityLogService.log(msg, "UPDATE", task.getHouse(), actor);
            }
        } else {
            // Al reabrir, limpiamos las auditorías de cierre de la base de datos
            task.setCompletedBy(null);
            task.setCompletedAt(null);

            String msg = String.format("%s reabrió el deber '%s'", actor.getUsername(), task.getTitle());
            activityLogService.log(msg, "UPDATE", task.getHouse(), actor);
        }

        taskRepository.save(task);
    }
}