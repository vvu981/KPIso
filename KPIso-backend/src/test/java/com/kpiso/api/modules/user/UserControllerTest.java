package com.kpiso.api.modules.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kpiso.api.modules.user.dto.DeleteUserRequest;
import com.kpiso.api.modules.user.dto.UpdateUserRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void updateUserShouldReturnOk() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("newUsername")
                .email("new@email.com")
                .build();
        User user = User.builder().id(userId).username("newUsername").email("new@email.com").build();

        when(userService.updateUser(eq(userId), any(UpdateUserRequest.class))).thenReturn(user);

        mockMvc.perform(put("/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateUserShouldReturnBadRequestOnError() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("newUsername")
                .email("new@email.com")
                .build();

        when(userService.updateUser(eq(userId), any(UpdateUserRequest.class)))
                .thenThrow(new IllegalArgumentException("Error updating user"));

        mockMvc.perform(put("/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error updating user"));
    }

    @Test
    void deleteUserShouldReturnOk() throws Exception {
        UUID userId = UUID.randomUUID();
        DeleteUserRequest request = new DeleteUserRequest();
        request.setPassword("password");

        doNothing().when(userService).deleteUser(eq(userId), any(DeleteUserRequest.class));

        mockMvc.perform(delete("/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Cuenta eliminada correctamente"));
    }

    @Test
    void deleteUserShouldReturnBadRequestOnError() throws Exception {
        UUID userId = UUID.randomUUID();
        DeleteUserRequest request = new DeleteUserRequest();
        request.setPassword("password");

        doThrow(new IllegalArgumentException("Error deleting user")).when(userService).deleteUser(eq(userId), any(DeleteUserRequest.class));

        mockMvc.perform(delete("/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error deleting user"));
    }
}
