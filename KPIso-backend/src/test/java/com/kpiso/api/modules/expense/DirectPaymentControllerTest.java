package com.kpiso.api.modules.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kpiso.api.modules.expense.dto.CreateDirectPaymentRequest;
import com.kpiso.api.modules.expense.dto.DirectPaymentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DirectPaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class DirectPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DirectPaymentService directPaymentService;

    @Test
    void createDirectPaymentShouldReturnCreated() throws Exception {
        CreateDirectPaymentRequest request = new CreateDirectPaymentRequest();
        request.setAmount(new BigDecimal("5.00"));
        request.setHouseId(UUID.randomUUID());
        request.setSenderId(UUID.randomUUID());
        request.setRecipientId(UUID.randomUUID());

        when(directPaymentService.createDirectPayment(any(CreateDirectPaymentRequest.class)))
                .thenReturn(DirectPaymentResponse.builder().build());

        mockMvc.perform(post("/direct-payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void getHouseDirectPaymentsShouldReturnList() throws Exception {
        UUID houseId = UUID.randomUUID();
        when(directPaymentService.getHouseDirectPayments(houseId)).thenReturn(List.of());

        mockMvc.perform(get("/direct-payments/house/" + houseId))
                .andExpect(status().isOk());
    }

    @Test
    void updateDirectPaymentShouldReturnOk() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CreateDirectPaymentRequest request = new CreateDirectPaymentRequest();
        request.setAmount(new BigDecimal("10.00"));
        request.setHouseId(UUID.randomUUID());
        request.setSenderId(UUID.randomUUID());
        request.setRecipientId(UUID.randomUUID());

        when(directPaymentService.updateDirectPayment(eq(paymentId), any(CreateDirectPaymentRequest.class), eq(userId)))
                .thenReturn(DirectPaymentResponse.builder().build());

        mockMvc.perform(put("/direct-payments/" + paymentId)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteDirectPaymentShouldReturnOk() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        doNothing().when(directPaymentService).deleteDirectPayment(paymentId, userId);

        mockMvc.perform(delete("/direct-payments/" + paymentId).param("userId", userId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void deleteDirectPaymentShouldReturnBadRequestOnError() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        doThrow(new IllegalArgumentException("Error")).when(directPaymentService).deleteDirectPayment(paymentId, userId);

        mockMvc.perform(delete("/direct-payments/" + paymentId).param("userId", userId.toString()))
                .andExpect(status().isBadRequest());
    }
}
