package com.kpiso.api.modules.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kpiso.api.modules.expense.dto.CreateExpenseRequest;
import com.kpiso.api.modules.expense.dto.ExpenseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExpenseController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExpenseService expenseService;

    @Test
    void createExpenseShouldReturnCreated() throws Exception {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("Gasto")
                .amount(new BigDecimal("10.00"))
                .houseId(UUID.randomUUID())
                .paidById(UUID.randomUUID())
                .participantIds(List.of(UUID.randomUUID()))
                .build();
        when(expenseService.createExpense(any(CreateExpenseRequest.class))).thenReturn(ExpenseResponse.builder().build());

        mockMvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void updateExpenseShouldReturnOk() throws Exception {
        UUID expenseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("Gasto")
                .amount(new BigDecimal("10.00"))
                .houseId(UUID.randomUUID())
                .paidById(UUID.randomUUID())
                .participantIds(List.of(UUID.randomUUID()))
                .build();
        when(expenseService.updateExpense(eq(expenseId), any(CreateExpenseRequest.class), eq(userId)))
                .thenReturn(ExpenseResponse.builder().build());

        mockMvc.perform(put("/expenses/" + expenseId)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void getHouseExpensesShouldReturnList() throws Exception {
        UUID houseId = UUID.randomUUID();
        when(expenseService.getHouseExpenses(houseId)).thenReturn(List.of());

        mockMvc.perform(get("/expenses/house/" + houseId))
                .andExpect(status().isOk());
    }

    @Test
    void getHouseMemberStatusesShouldReturnMap() throws Exception {
        UUID houseId = UUID.randomUUID();
        when(expenseService.getHouseMemberStatuses(houseId)).thenReturn(Map.of());

        mockMvc.perform(get("/expenses/house/" + houseId + "/statuses"))
                .andExpect(status().isOk());
    }

    @Test
    void getSettlementsShouldReturnList() throws Exception {
        UUID houseId = UUID.randomUUID();
        when(expenseService.calculateSettlements(houseId)).thenReturn(List.of());

        mockMvc.perform(get("/expenses/house/" + houseId + "/settlement"))
                .andExpect(status().isOk());
    }

    @Test
    void settleAllShouldReturnOk() throws Exception {
        UUID houseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        doNothing().when(expenseService).settleAllHouseExpenses(houseId, userId);

        mockMvc.perform(post("/expenses/house/" + houseId + "/settle-all").param("userId", userId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void settleAllShouldReturnBadRequestOnError() throws Exception {
        UUID houseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        doThrow(new IllegalArgumentException("Error")).when(expenseService).settleAllHouseExpenses(houseId, userId);

        mockMvc.perform(post("/expenses/house/" + houseId + "/settle-all").param("userId", userId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteExpenseShouldReturnOk() throws Exception {
        UUID expenseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        doNothing().when(expenseService).deleteExpense(expenseId, userId);

        mockMvc.perform(delete("/expenses/" + expenseId).param("userId", userId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void deleteExpenseShouldReturnBadRequestOnError() throws Exception {
        UUID expenseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        doThrow(new IllegalArgumentException("Error")).when(expenseService).deleteExpense(expenseId, userId);

        mockMvc.perform(delete("/expenses/" + expenseId).param("userId", userId.toString()))
                .andExpect(status().isBadRequest());
    }
}
