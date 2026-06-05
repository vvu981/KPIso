package com.kpiso.api.modules.task;

import com.kpiso.api.modules.activity.ActivityLogService;
import com.kpiso.api.modules.house.House;
import com.kpiso.api.modules.house.HouseMember;
import com.kpiso.api.modules.house.HouseMemberRepository;
import com.kpiso.api.modules.house.HouseRepository;
import com.kpiso.api.modules.house.HouseRole;
import com.kpiso.api.modules.task.dto.CreateTaskRequest;
import com.kpiso.api.modules.task.dto.TaskResponse;
import com.kpiso.api.modules.user.User;
import com.kpiso.api.modules.user.UserRepository;
import com.kpiso.api.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private HouseRepository houseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HouseMemberRepository houseMemberRepository;

        private ActivityLogService activityLogService;

    private TaskService taskService;

    private House house;
    private User creator;
    private User assignee;
    private User rescuer;
    private User secondAssignee;

    @BeforeEach
    void setUp() {
        house = TestFixtures.house("Piso", "INV123");
        creator = TestFixtures.user("creator", "creator@email.com");
        assignee = TestFixtures.user("assignee", "assignee@email.com");
        rescuer = TestFixtures.user("rescuer", "rescuer@email.com");
        secondAssignee = TestFixtures.user("second", "second@email.com");
        activityLogService = new ActivityLogService(null) {
            @Override
            public void log(String description, String actionType, House house, User user) {
            }
        };
        taskService = new TaskService(taskRepository, houseRepository, userRepository, houseMemberRepository, activityLogService);
    }

    @Test
    void createTaskShouldCreateFixedTask() {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Limpiar")
                .description("Cocina")
                .points(3)
                .houseId(house.getId())
                .rotationType(RotationType.FIXED)
                .assignedToId(assignee.getId())
                .occurrencesToProject(1)
                .startDate(TestFixtures.localDateTime(2026, 5, 10, 9, 0))
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(houseMemberRepository.findByHouseIdAndUserId(house.getId(), assignee.getId()))
                .thenReturn(Optional.of(TestFixtures.houseMember(house, assignee, HouseRole.MEMBER, "#10b981")));

        List<TaskResponse> responses = taskService.createTask(request);

        assertEquals(1, responses.size());
        assertEquals("Limpiar", responses.get(0).getTitle());
        assertEquals(TaskStatus.PENDING, responses.get(0).getStatus());
        assertEquals(RotationType.FIXED, responses.get(0).getRotationType());
        assertEquals(assignee.getUsername(), responses.get(0).getAssignedTo().getUsername());
    }

    @Test
    void createTaskShouldFailForFixedTaskWithoutAssignee() {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Limpiar")
                .points(3)
                .houseId(house.getId())
                .rotationType(RotationType.FIXED)
                .occurrencesToProject(1)
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> taskService.createTask(request));

        assertEquals("Se debe asignar un usuario para tareas de tipo fijo", exception.getMessage());
    }

    @Test
    void createTaskShouldFailWhenAssigneeDoesNotExist() {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Limpiar")
                .points(3)
                .houseId(house.getId())
                .rotationType(RotationType.FIXED)
                .assignedToId(assignee.getId())
                .occurrencesToProject(1)
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> taskService.createTask(request));

        assertEquals("El usuario asignado no existe", exception.getMessage());
    }

    @Test
    void createTaskShouldCreateDailyRotatingTasks() {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Rotación diaria")
                .description("Tareas compartidas")
                .points(2)
                .houseId(house.getId())
                .rotationType(RotationType.DAILY)
                .participantIds(List.of(assignee.getId(), secondAssignee.getId()))
                .occurrencesToProject(3)
                .startDate(TestFixtures.localDateTime(2026, 5, 10, 9, 0))
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));
        when(userRepository.findById(secondAssignee.getId())).thenReturn(Optional.of(secondAssignee));
        when(houseMemberRepository.existsByHouseIdAndUserId(house.getId(), assignee.getId())).thenReturn(true);
        when(houseMemberRepository.existsByHouseIdAndUserId(house.getId(), secondAssignee.getId())).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(houseMemberRepository.findByHouseIdAndUserId(eq(house.getId()), any(UUID.class)))
                .thenAnswer(invocation -> Optional.of(TestFixtures.houseMember(house, assignee, HouseRole.MEMBER, "#6366f1")));

        List<TaskResponse> responses = taskService.createTask(request);

        assertEquals(3, responses.size());
        assertEquals("Rotación diaria (Turno 1)", responses.get(0).getTitle());
        assertEquals(request.getStartDate(), responses.get(0).getDueDate());
        assertEquals(request.getStartDate().plusDays(1), responses.get(1).getDueDate());
        assertEquals(request.getStartDate().plusDays(2), responses.get(2).getDueDate());
    }

    @Test
    void createTaskShouldCreateWeeklyRotatingTasksOnSpecificDays() {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Rotación semanal")
                .points(2)
                .houseId(house.getId())
                .rotationType(RotationType.WEEKLY)
                .participantIds(List.of(assignee.getId()))
                .specificDays(List.of(3))
                .occurrencesToProject(2)
                .startDate(TestFixtures.localDateTime(2026, 5, 11, 9, 0))
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));
        when(houseMemberRepository.existsByHouseIdAndUserId(house.getId(), assignee.getId())).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(houseMemberRepository.findByHouseIdAndUserId(eq(house.getId()), any(UUID.class)))
                .thenReturn(Optional.of(TestFixtures.houseMember(house, assignee, HouseRole.MEMBER, "#6366f1")));

        List<TaskResponse> responses = taskService.createTask(request);

        assertEquals(2, responses.size());
        assertTrue(request.getSpecificDays().contains(responses.get(0).getDueDate().getDayOfWeek().getValue()));
        assertTrue(request.getSpecificDays().contains(responses.get(1).getDueDate().getDayOfWeek().getValue()));
    }

    @Test
    void createTaskShouldCreateMonthlyRotatingTasks() {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Rotación mensual")
                .points(2)
                .houseId(house.getId())
                .rotationType(RotationType.MONTHLY)
                .participantIds(List.of(assignee.getId()))
                .occurrencesToProject(2)
                .startDate(TestFixtures.localDateTime(2026, 5, 15, 9, 0))
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));
        when(houseMemberRepository.existsByHouseIdAndUserId(house.getId(), assignee.getId())).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(houseMemberRepository.findByHouseIdAndUserId(eq(house.getId()), any(UUID.class)))
                .thenReturn(Optional.of(TestFixtures.houseMember(house, assignee, HouseRole.MEMBER, "#6366f1")));

        List<TaskResponse> responses = taskService.createTask(request);

        assertEquals(2, responses.size());
        assertEquals(request.getStartDate().plusMonths(1), responses.get(1).getDueDate());
    }

    @Test
    void createTaskShouldFailWhenRotatingWithoutParticipants() {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Rotación")
                .points(2)
                .houseId(house.getId())
                .rotationType(RotationType.DAILY)
                .occurrencesToProject(1)
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(houseMemberRepository.findByHouseId(house.getId())).thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> taskService.createTask(request));

        assertEquals("No se pueden crear tareas rotativas sin participantes", exception.getMessage());
    }

    @Test
    void createTaskShouldFailWhenParticipantIsNotMember() {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Rotación")
                .points(2)
                .houseId(house.getId())
                .rotationType(RotationType.DAILY)
                .participantIds(List.of(assignee.getId()))
                .occurrencesToProject(1)
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(houseMemberRepository.existsByHouseIdAndUserId(house.getId(), assignee.getId())).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> taskService.createTask(request));

        assertEquals("El usuario no pertenece a esta casa", exception.getMessage());
    }

    @Test
    void getTasksByHouseShouldMapActiveTasks() {
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title("Limpiar")
                .description("Cocina")
                .points(3)
                .status(TaskStatus.PENDING)
                .rotationType(RotationType.FIXED)
                .dueDate(TestFixtures.localDateTime(2026, 5, 10, 9, 0))
                .house(house)
                .assignedTo(assignee)
                .build();

        when(taskRepository.findByHouseIdAndDeletedAtIsNull(house.getId())).thenReturn(List.of(task));
        when(houseMemberRepository.findByHouseIdAndUserId(house.getId(), assignee.getId()))
                .thenReturn(Optional.of(TestFixtures.houseMember(house, assignee, HouseRole.MEMBER, "#10b981")));

        List<TaskResponse> responses = taskService.getTasksByHouse(house.getId());

        assertEquals(1, responses.size());
        assertEquals("Limpiar", responses.get(0).getTitle());
        assertNotNull(responses.get(0).getAssignedTo());
    }

    @Test
    void updateTaskStatusShouldChangeStatus() {
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title("Limpiar")
                .points(3)
                .status(TaskStatus.PENDING)
                .rotationType(RotationType.FIXED)
                .house(house)
                .build();

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateTaskStatus(task.getId(), TaskStatus.COMPLETED);

        assertEquals(TaskStatus.COMPLETED, response.getStatus());
    }

    @Test
    void updateTaskStatusShouldFailWhenTaskDoesNotExist() {
        when(taskRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> taskService.updateTaskStatus(UUID.randomUUID(), TaskStatus.COMPLETED));

        assertEquals("La tarea no existe", exception.getMessage());
    }

    @Test
    void updateTaskDueDateShouldChangeDueDateAndLogAction() {
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title("Limpiar")
                .points(3)
                .status(TaskStatus.PENDING)
                .rotationType(RotationType.FIXED)
                .dueDate(TestFixtures.localDateTime(2026, 5, 10, 9, 0))
                .house(house)
                .build();

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateTaskDueDate(task.getId(), TestFixtures.localDateTime(2026, 5, 11, 9, 0), creator.getId());

        assertEquals(TestFixtures.localDateTime(2026, 5, 11, 9, 0), response.getDueDate());
    }

    @Test
    void updateTaskDueDateShouldFailWhenUserDoesNotExist() {
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title("Limpiar")
                .points(3)
                .status(TaskStatus.PENDING)
                .rotationType(RotationType.FIXED)
                .dueDate(TestFixtures.localDateTime(2026, 5, 10, 9, 0))
                .house(house)
                .build();

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findById(creator.getId())).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> taskService.updateTaskDueDate(task.getId(), TestFixtures.localDateTime(2026, 5, 11, 9, 0), creator.getId()));

        assertEquals("Usuario no válido", exception.getMessage());
    }

    @Test
    void updateTaskShouldUpdateTitleAssignmentAndDate() {
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title("Limpiar")
                .description("Original")
                .points(3)
                .status(TaskStatus.PENDING)
                .rotationType(RotationType.DAILY)
                .dueDate(TestFixtures.localDateTime(2026, 5, 10, 9, 0))
                .house(house)
                .assignedTo(assignee)
                .build();

        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Limpiar otra vez")
                .description("Actualizada")
                .points(5)
                .rotationType(RotationType.MONTHLY)
                .startDate(TestFixtures.localDateTime(2026, 5, 20, 9, 0))
                .assignedToId(secondAssignee.getId())
                .build();

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(userRepository.findById(secondAssignee.getId())).thenReturn(Optional.of(secondAssignee));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(houseMemberRepository.findByHouseIdAndUserId(house.getId(), secondAssignee.getId()))
                .thenReturn(Optional.of(TestFixtures.houseMember(house, secondAssignee, HouseRole.MEMBER, "#10b981")));

        TaskResponse response = taskService.updateTask(task.getId(), request, creator.getId());

        assertEquals("Limpiar otra vez", response.getTitle());
        assertEquals(TaskStatus.PENDING, response.getStatus());
        assertEquals(RotationType.MONTHLY, response.getRotationType());
        assertEquals(secondAssignee.getUsername(), response.getAssignedTo().getUsername());
        assertEquals(TestFixtures.localDateTime(2026, 5, 20, 9, 0), response.getDueDate());
    }

    @Test
    void updateTaskShouldClearAssignmentWhenRequestDoesNotProvideAssignee() {
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title("Limpiar")
                .description("Original")
                .points(3)
                .status(TaskStatus.PENDING)
                .rotationType(RotationType.DAILY)
                .dueDate(TestFixtures.localDateTime(2026, 5, 10, 9, 0))
                .house(house)
                .assignedTo(assignee)
                .build();

        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Limpiar otra vez")
                .description("Actualizada")
                .points(5)
                .rotationType(RotationType.MONTHLY)
                .build();

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateTask(task.getId(), request, creator.getId());

        assertNull(response.getAssignedTo());
    }

    @Test
    void softDeleteTaskShouldMarkTaskAsDeleted() {
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title("Limpiar")
                .points(3)
                .status(TaskStatus.PENDING)
                .rotationType(RotationType.DAILY)
                .house(house)
                .build();

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taskService.softDeleteTask(task.getId(), creator.getId());

        assertNotNull(task.getDeletedAt());
    }

    @Test
    void toggleTaskStatusShouldHandleCompletedLateRescue() {
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title("Tarea")
                .points(4)
                .status(TaskStatus.PENDING)
                .rotationType(RotationType.FIXED)
                .dueDate(TestFixtures.localDateTime(2026, 5, 1, 9, 0))
                .house(house)
                .assignedTo(assignee)
                .build();

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findById(rescuer.getId())).thenReturn(Optional.of(rescuer));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taskService.toggleTaskStatus(task.getId(), "completed", rescuer.getId());

        assertEquals(TaskStatus.COMPLETED, task.getStatus());
        assertEquals(rescuer, task.getCompletedBy());
        assertNotNull(task.getCompletedAt());
    }

    @Test
    void toggleTaskStatusShouldHandleCompletedOwnTask() {
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title("Tarea")
                .points(4)
                .status(TaskStatus.PENDING)
                .rotationType(RotationType.FIXED)
                .dueDate(TestFixtures.localDateTime(2026, 5, 1, 9, 0))
                .house(house)
                .assignedTo(rescuer)
                .build();

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findById(rescuer.getId())).thenReturn(Optional.of(rescuer));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taskService.toggleTaskStatus(task.getId(), "COMPLETED", rescuer.getId());

        assertEquals(TaskStatus.COMPLETED, task.getStatus());
        assertEquals(rescuer, task.getCompletedBy());
    }

    @Test
    void toggleTaskStatusShouldReopenTask() {
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title("Tarea")
                .points(4)
                .status(TaskStatus.COMPLETED)
                .rotationType(RotationType.FIXED)
                .dueDate(TestFixtures.localDateTime(2026, 5, 1, 9, 0))
                .house(house)
                .assignedTo(rescuer)
                .completedBy(rescuer)
                .completedAt(TestFixtures.localDateTime(2026, 5, 2, 9, 0))
                .build();

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findById(rescuer.getId())).thenReturn(Optional.of(rescuer));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taskService.toggleTaskStatus(task.getId(), "pending", rescuer.getId());

        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertNull(task.getCompletedBy());
        assertNull(task.getCompletedAt());
    }

    @Test
    void toggleTaskStatusShouldFailForInvalidStatus() {
        Task task = Task.builder().id(UUID.randomUUID()).title("Tarea").points(4).status(TaskStatus.PENDING).rotationType(RotationType.FIXED).house(house).build();

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        assertThrows(IllegalArgumentException.class, () -> taskService.toggleTaskStatus(task.getId(), "invalid", rescuer.getId()));
    }

    @Test
    void createTaskShouldFallbackToDailyWhenRotationTypeIsUnknown() {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Rotación Rara")
                .points(2)
                .houseId(house.getId())
                .rotationType(null) // default daily
                .occurrencesToProject(1)
                .startDate(TestFixtures.localDateTime(2026, 5, 10, 9, 0))
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        HouseMember member = TestFixtures.houseMember(house, assignee, HouseRole.MEMBER, "#10b981");
        when(houseMemberRepository.findByHouseId(house.getId())).thenReturn(List.of(member));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(houseMemberRepository.findByHouseIdAndUserId(eq(house.getId()), any(UUID.class)))
                .thenReturn(Optional.of(member));

        List<TaskResponse> responses = taskService.createTask(request);

        assertEquals(1, responses.size());
        assertEquals("Rotación Rara", responses.get(0).getTitle());
    }

    @Test
    void createTaskShouldFallbackToStartDateWeekdayWhenWeeklySpecificDaysIsEmpty() {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Semanal sin Días")
                .points(2)
                .houseId(house.getId())
                .rotationType(RotationType.WEEKLY)
                .occurrencesToProject(1)
                .startDate(TestFixtures.localDateTime(2026, 5, 10, 9, 0)) // 10 de Mayo de 2026 es Domingo (7)
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        HouseMember member = TestFixtures.houseMember(house, assignee, HouseRole.MEMBER, "#10b981");
        when(houseMemberRepository.findByHouseId(house.getId())).thenReturn(List.of(member));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(houseMemberRepository.findByHouseIdAndUserId(eq(house.getId()), any(UUID.class)))
                .thenReturn(Optional.of(member));

        List<TaskResponse> responses = taskService.createTask(request);

        assertEquals(1, responses.size());
        assertEquals(7, responses.get(0).getDueDate().getDayOfWeek().getValue());
    }

    @Test
    void createTaskShouldFailWhenParticipantUserDoesNotExist() {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Rotación")
                .points(2)
                .houseId(house.getId())
                .rotationType(RotationType.DAILY)
                .participantIds(List.of(assignee.getId()))
                .occurrencesToProject(1)
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(houseMemberRepository.existsByHouseIdAndUserId(house.getId(), assignee.getId())).thenReturn(true);
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> taskService.createTask(request));
    }

    @Test
    void executeDailyTaskRotationShouldRotateTasksAndChainNextTask() {
        Task oldTask = Task.builder()
                .id(UUID.randomUUID())
                .title("Limpiar Plato")
                .rotationType(RotationType.DAILY)
                .dueDate(LocalDateTime.now().minusDays(1))
                .house(house)
                .assignedTo(assignee)
                .build();

        HouseMember member1 = TestFixtures.houseMember(house, assignee, HouseRole.MEMBER, "#111111");
        HouseMember member2 = TestFixtures.houseMember(house, secondAssignee, HouseRole.MEMBER, "#222222");

        when(taskRepository.findByRotationTypeNotAndNextTaskIdIsNullAndDueDateLessThanEqualAndDeletedAtIsNull(any(), any()))
                .thenReturn(List.of(oldTask));
        when(houseMemberRepository.findByHouseId(house.getId())).thenReturn(List.of(member1, member2));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> {
            Task t = i.getArgument(0);
            if (t.getId() == null) {
                t.setId(UUID.randomUUID());
            }
            return t;
        });

        taskService.executeDailyTaskRotation();

        verify(taskRepository, times(2)).save(any(Task.class));
        assertNotNull(oldTask.getNextTaskId());
    }

    @Test
    void executeDailyTaskRotationShouldSkipIfNoActiveMembers() {
        Task oldTask = Task.builder()
                .id(UUID.randomUUID())
                .rotationType(RotationType.DAILY)
                .dueDate(LocalDateTime.now().minusDays(1))
                .house(house)
                .assignedTo(assignee)
                .build();

        when(taskRepository.findByRotationTypeNotAndNextTaskIdIsNullAndDueDateLessThanEqualAndDeletedAtIsNull(any(), any()))
                .thenReturn(List.of(oldTask));
        when(houseMemberRepository.findByHouseId(house.getId())).thenReturn(List.of());

        taskService.executeDailyTaskRotation();

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void executeDailyTaskRotationShouldAssignToFirstIfCurrentAssigneeNotFound() {
        Task oldTask = Task.builder()
                .id(UUID.randomUUID())
                .rotationType(RotationType.WEEKLY)
                .dueDate(LocalDateTime.now().minusDays(1))
                .house(house)
                .assignedTo(null)
                .build();

        HouseMember member = TestFixtures.houseMember(house, assignee, HouseRole.MEMBER, "#111111");

        when(taskRepository.findByRotationTypeNotAndNextTaskIdIsNullAndDueDateLessThanEqualAndDeletedAtIsNull(any(), any()))
                .thenReturn(List.of(oldTask));
        when(houseMemberRepository.findByHouseId(house.getId())).thenReturn(List.of(member));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> {
            Task t = i.getArgument(0);
            if (t.getId() == null) {
                t.setId(UUID.randomUUID());
            }
            return t;
        });

        taskService.executeDailyTaskRotation();

        verify(taskRepository, times(2)).save(any(Task.class));
    }

    @Test
    void toggleTaskStatusShouldFailWhenTaskNotFound() {
        UUID taskId = UUID.randomUUID();
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> taskService.toggleTaskStatus(taskId, "COMPLETED", assignee.getId()));
    }

    @Test
    void toggleTaskStatusShouldFailWhenUserNotFound() {
        Task task = Task.builder().id(UUID.randomUUID()).title("Tarea").points(4).status(TaskStatus.PENDING).rotationType(RotationType.FIXED).house(house).build();
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> taskService.toggleTaskStatus(task.getId(), "COMPLETED", assignee.getId()));
    }

    @Test
    void updateTaskDueDateShouldFailWhenTaskNotFound() {
        UUID taskId = UUID.randomUUID();
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> taskService.updateTaskDueDate(taskId, LocalDateTime.now(), assignee.getId()));
    }

    @Test
    void updateTaskShouldFailWhenTaskNotFound() {
        UUID taskId = UUID.randomUUID();
        CreateTaskRequest request = CreateTaskRequest.builder().build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> taskService.updateTask(taskId, request, assignee.getId()));
    }

    @Test
    void updateTaskShouldFailWhenActorNotFound() {
        Task task = Task.builder().id(UUID.randomUUID()).title("Tarea").points(4).status(TaskStatus.PENDING).rotationType(RotationType.FIXED).house(house).build();
        CreateTaskRequest request = CreateTaskRequest.builder().build();
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> taskService.updateTask(task.getId(), request, assignee.getId()));
    }

    @Test
    void updateTaskShouldFailWhenNewAssigneeNotFound() {
        Task task = Task.builder().id(UUID.randomUUID()).title("Tarea").points(4).status(TaskStatus.PENDING).rotationType(RotationType.FIXED).house(house).build();
        CreateTaskRequest request = CreateTaskRequest.builder().assignedToId(secondAssignee.getId()).build();
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));
        when(userRepository.findById(secondAssignee.getId())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> taskService.updateTask(task.getId(), request, assignee.getId()));
    }

    @Test
    void softDeleteTaskShouldFailWhenTaskNotFound() {
        UUID taskId = UUID.randomUUID();
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> taskService.softDeleteTask(taskId, assignee.getId()));
    }

    @Test
    void softDeleteTaskShouldFailWhenActorNotFound() {
        Task task = Task.builder().id(UUID.randomUUID()).title("Tarea").points(4).status(TaskStatus.PENDING).rotationType(RotationType.FIXED).house(house).build();
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> taskService.softDeleteTask(task.getId(), assignee.getId()));
    }
}