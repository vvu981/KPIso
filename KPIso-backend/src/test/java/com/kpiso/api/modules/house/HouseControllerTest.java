package com.kpiso.api.modules.house;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kpiso.api.modules.house.dto.CreateHouseRequest;
import com.kpiso.api.modules.house.dto.HouseDetailResponse;
import com.kpiso.api.modules.house.dto.JoinHouseRequest;
import com.kpiso.api.modules.house.dto.UserHouseResponse;
import com.kpiso.api.modules.house.stats.HouseStatisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HouseController.class)
@AutoConfigureMockMvc(addFilters = false)
class HouseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HouseService houseService;

    @MockBean
    private HouseStatisticsService houseStatisticsService;

    @Test
    void createHouseShouldReturnCreated() throws Exception {
        CreateHouseRequest request = CreateHouseRequest.builder()
                .name("Mi Hogar")
                .creatorId(UUID.randomUUID())
                .build();
        House house = House.builder().id(UUID.randomUUID()).name("Mi Hogar").build();

        when(houseService.createHouse(any(CreateHouseRequest.class))).thenReturn(house);

        mockMvc.perform(post("/houses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createHouseShouldReturnBadRequestOnError() throws Exception {
        CreateHouseRequest request = CreateHouseRequest.builder()
                .name("Mi Hogar")
                .creatorId(UUID.randomUUID())
                .build();

        when(houseService.createHouse(any(CreateHouseRequest.class))).thenThrow(new IllegalArgumentException("Error"));

        mockMvc.perform(post("/houses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error"));
    }

    @Test
    void joinHouseShouldReturnOk() throws Exception {
        JoinHouseRequest request = new JoinHouseRequest();
        request.setInviteCode("ABC123");
        request.setUserId(UUID.randomUUID());

        House house = House.builder().id(UUID.randomUUID()).name("Mi Hogar").build();
        when(houseService.joinHouse(eq("ABC123"), any(UUID.class))).thenReturn(house);

        mockMvc.perform(post("/houses/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void joinHouseShouldReturnBadRequestOnError() throws Exception {
        JoinHouseRequest request = new JoinHouseRequest();
        request.setInviteCode("ABC123");
        request.setUserId(UUID.randomUUID());

        when(houseService.joinHouse(eq("ABC123"), any(UUID.class))).thenThrow(new IllegalArgumentException("Invalid code"));

        mockMvc.perform(post("/houses/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHouseDetailShouldReturnDetail() throws Exception {
        UUID houseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        HouseDetailResponse detail = HouseDetailResponse.builder().id(houseId).name("Hogar").build();

        when(houseService.getHouseDetail(eq(houseId), eq(userId))).thenReturn(detail);

        mockMvc.perform(get("/houses/" + houseId).param("userId", userId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void getHouseDetailShouldReturnForbiddenOnAccessDenied() throws Exception {
        UUID houseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(houseService.getHouseDetail(eq(houseId), eq(userId))).thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(get("/houses/" + houseId).param("userId", userId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getHouseDetailShouldReturnNotFoundOnException() throws Exception {
        UUID houseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(houseService.getHouseDetail(eq(houseId), eq(userId))).thenThrow(new IllegalArgumentException("Not found"));

        mockMvc.perform(get("/houses/" + houseId).param("userId", userId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserHousesShouldReturnList() throws Exception {
        UUID userId = UUID.randomUUID();
        when(houseService.getUserHouses(userId)).thenReturn(List.of());

        mockMvc.perform(get("/houses/user/" + userId))
                .andExpect(status().isOk());
    }

    @Test
    void getDeletedHousesShouldReturnList() throws Exception {
        UUID userId = UUID.randomUUID();
        when(houseService.getDeletedUserHouses(userId)).thenReturn(List.of());

        mockMvc.perform(get("/houses/user/" + userId + "/deleted"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteHouseShouldReturnOk() throws Exception {
        UUID houseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        doNothing().when(houseService).softDeleteHouse(houseId, userId);

        mockMvc.perform(delete("/houses/" + houseId).param("userId", userId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void deleteHouseShouldReturnBadRequestOnError() throws Exception {
        UUID houseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        doThrow(new IllegalArgumentException("Error")).when(houseService).softDeleteHouse(houseId, userId);

        mockMvc.perform(delete("/houses/" + houseId).param("userId", userId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateHouseShouldReturnUpdatedDetail() throws Exception {
        UUID houseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CreateHouseRequest request = CreateHouseRequest.builder()
                .name("Nuevo")
                .creatorId(UUID.randomUUID())
                .build();
        HouseDetailResponse detail = HouseDetailResponse.builder().id(houseId).name("Nuevo").build();

        when(houseService.updateHouse(eq(houseId), any(CreateHouseRequest.class), eq(userId))).thenReturn(detail);

        mockMvc.perform(put("/houses/" + houseId)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void removeMemberShouldReturnOk() throws Exception {
        UUID houseId = UUID.randomUUID();
        UUID memberUserId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        doNothing().when(houseService).removeMember(houseId, memberUserId, userId);

        mockMvc.perform(delete("/houses/" + houseId + "/members/" + memberUserId).param("userId", userId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void removeMemberShouldReturnBadRequestOnError() throws Exception {
        UUID houseId = UUID.randomUUID();
        UUID memberUserId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        doThrow(new IllegalArgumentException("Error")).when(houseService).removeMember(houseId, memberUserId, userId);

        mockMvc.perform(delete("/houses/" + houseId + "/members/" + memberUserId).param("userId", userId.toString()))
                .andExpect(status().isBadRequest());
    }
}
