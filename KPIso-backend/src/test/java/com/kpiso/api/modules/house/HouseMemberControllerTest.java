package com.kpiso.api.modules.house;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HouseMemberController.class)
@AutoConfigureMockMvc(addFilters = false)
class HouseMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HouseMemberRepository houseMemberRepository;

    @Test
    void updateMemberColorShouldReturnOkWhenValid() throws Exception {
        UUID houseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        HouseMember member = new HouseMember();

        when(houseMemberRepository.findByHouseIdAndUserId(eq(houseId), eq(userId)))
                .thenReturn(Optional.of(member));

        mockMvc.perform(patch("/houses/" + houseId + "/members/color")
                        .param("userId", userId.toString())
                        .param("color", "#FF0000"))
                .andExpect(status().isOk());

        verify(houseMemberRepository, times(1)).save(member);
    }

    @Test
    void updateMemberColorShouldReturnBadRequestWhenColorInvalid() throws Exception {
        UUID houseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        HouseMember member = new HouseMember();

        when(houseMemberRepository.findByHouseIdAndUserId(eq(houseId), eq(userId)))
                .thenReturn(Optional.of(member));

        mockMvc.perform(patch("/houses/" + houseId + "/members/color")
                        .param("userId", userId.toString())
                        .param("color", "invalid-color"))
                .andExpect(status().isBadRequest());

        verify(houseMemberRepository, never()).save(any());
    }

    @Test
    void updateMemberColorShouldReturnNotFoundWhenMemberDoesNotExist() throws Exception {
        UUID houseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(houseMemberRepository.findByHouseIdAndUserId(eq(houseId), eq(userId)))
                .thenReturn(Optional.empty());

        mockMvc.perform(patch("/houses/" + houseId + "/members/color")
                        .param("userId", userId.toString())
                        .param("color", "#FF0000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateSettleApprovalShouldReturnOkWhenFound() throws Exception {
        UUID houseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        HouseMember member = new HouseMember();

        when(houseMemberRepository.findByHouseIdAndUserId(eq(houseId), eq(userId)))
                .thenReturn(Optional.of(member));

        mockMvc.perform(patch("/houses/" + houseId + "/members/settle-approval")
                        .param("userId", userId.toString())
                        .param("approved", "true"))
                .andExpect(status().isOk());

        verify(houseMemberRepository, times(1)).save(member);
    }

    @Test
    void updateSettleApprovalShouldReturnNotFoundWhenMemberDoesNotExist() throws Exception {
        UUID houseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(houseMemberRepository.findByHouseIdAndUserId(eq(houseId), eq(userId)))
                .thenReturn(Optional.empty());

        mockMvc.perform(patch("/houses/" + houseId + "/members/settle-approval")
                        .param("userId", userId.toString())
                        .param("approved", "true"))
                .andExpect(status().isNotFound());
    }
}
