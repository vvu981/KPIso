package com.kpiso.api.modules.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kpiso.api.modules.task.dto.CreateTaskRequest;
import com.kpiso.api.modules.task.dto.TaskResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @Test
    void getTasksByHouseShouldReturnList() throws Exception {
        UUID houseId = UUID.randomUUID();
        when(taskService.getTasksByHouse(houseId)).thenReturn(List.of());

        mockMvc.perform(get("/tasks/house/" + houseId))
                .andExpect(status().isOk());
    }

    @Test
    void createTaskShouldReturnList() throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder().title("Task").build();
        when(taskService.createTask(any(CreateTaskRequest.class))).thenReturn(List.of());

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void toggleTaskStatusShouldReturnOk() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        doNothing().when(taskService).toggleTaskStatus(taskId, "COMPLETED", userId);

        mockMvc.perform(patch("/tasks/" + taskId + "/status")
                        .param("status", "COMPLETED")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void toggleTaskStatusShouldReturnBadRequestOnError() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        doThrow(new IllegalArgumentException("Error")).when(taskService).toggleTaskStatus(taskId, "COMPLETED", userId);

        mockMvc.perform(patch("/tasks/" + taskId + "/status")
                        .param("status", "COMPLETED")
                        .param("userId", userId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTaskDueDateShouldReturnResponse() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        when(taskService.updateTaskDueDate(eq(taskId), any(LocalDateTime.class), eq(userId)))
                .thenReturn(TaskResponse.builder().build());

        mockMvc.perform(patch("/tasks/" + taskId + "/due-date")
                        .param("dueDate", now.toString())
                        .param("userId", userId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void updateTaskShouldReturnResponse() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CreateTaskRequest request = CreateTaskRequest.builder().title("Updated").build();

        when(taskService.updateTask(eq(taskId), any(CreateTaskRequest.class), eq(userId)))
                .thenReturn(TaskResponse.builder().build());

        mockMvc.perform(put("/tasks/" + taskId)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteTaskShouldReturnOk() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        doNothing().when(taskService).softDeleteTask(taskId, userId);

        mockMvc.perform(delete("/tasks/" + taskId).param("userId", userId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void deleteTaskShouldReturnBadRequestOnError() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        doThrow(new IllegalArgumentException("Error")).when(taskService).softDeleteTask(taskId, userId);

        mockMvc.perform(delete("/tasks/" + taskId).param("userId", userId.toString()))
                .andExpect(status().isBadRequest());
    }
}
