package com.kpiso.api.modules.shoppinglist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kpiso.api.modules.shoppinglist.dto.AddShoppingItemRequest;
import com.kpiso.api.modules.shoppinglist.dto.CheckoutRequest;
import com.kpiso.api.modules.shoppinglist.dto.ShoppingItemResponse;
import com.kpiso.api.modules.shoppinglist.dto.ShoppingListResponse;
import com.kpiso.api.modules.shoppinglist.service.ShoppingListService;
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

@WebMvcTest(ShoppingListController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShoppingListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShoppingListService shoppingListService;

    @Test
    void searchProductSuggestionsShouldReturnSuggestions() throws Exception {
        when(shoppingListService.searchProductSuggestions("leche", false)).thenReturn(List.of());

        mockMvc.perform(get("/shopping-list/search").param("query", "leche"))
                .andExpect(status().isOk());
    }

    @Test
    void searchProductSuggestionsShouldReturnBadRequestIfQueryEmpty() throws Exception {
        mockMvc.perform(get("/shopping-list/search").param("query", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getShoppingListShouldReturnResponse() throws Exception {
        UUID houseId = UUID.randomUUID();
        when(shoppingListService.getShoppingList(houseId)).thenReturn(ShoppingListResponse.builder().build());

        mockMvc.perform(get("/shopping-list/" + houseId))
                .andExpect(status().isOk());
    }

    @Test
    void getPendingItemsShouldReturnList() throws Exception {
        UUID houseId = UUID.randomUUID();
        when(shoppingListService.getPendingItems(houseId)).thenReturn(List.of());

        mockMvc.perform(get("/shopping-list/" + houseId + "/pending"))
                .andExpect(status().isOk());
    }

    @Test
    void getBoughtItemsShouldReturnList() throws Exception {
        UUID houseId = UUID.randomUUID();
        when(shoppingListService.getBoughtItems(houseId)).thenReturn(List.of());

        mockMvc.perform(get("/shopping-list/" + houseId + "/bought"))
                .andExpect(status().isOk());
    }

    @Test
    void getEstimatedBudgetShouldReturnDouble() throws Exception {
        UUID houseId = UUID.randomUUID();
        when(shoppingListService.getEstimatedBudget(houseId)).thenReturn(10.0);

        mockMvc.perform(get("/shopping-list/" + houseId + "/budget"))
                .andExpect(status().isOk());
    }

    @Test
    void addShoppingItemShouldReturnCreated() throws Exception {
        AddShoppingItemRequest request = AddShoppingItemRequest.builder()
                .productName("Manzana")
                .houseId(UUID.randomUUID())
                .addedById(UUID.randomUUID())
                .build();
        when(shoppingListService.addShoppingItem(any(AddShoppingItemRequest.class)))
                .thenReturn(ShoppingItemResponse.builder().build());

        mockMvc.perform(post("/shopping-list/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void checkoutShoppingListShouldReturnOk() throws Exception {
        CheckoutRequest request = CheckoutRequest.builder()
                .houseId(UUID.randomUUID())
                .paidById(UUID.randomUUID())
                .totalRealAmount(new BigDecimal("15.00"))
                .build();

        doNothing().when(shoppingListService).checkoutShoppingList(any(CheckoutRequest.class));

        mockMvc.perform(post("/shopping-list/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void markAsBoughtShouldReturnResponse() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(shoppingListService.markAsBought(itemId)).thenReturn(ShoppingItemResponse.builder().build());

        mockMvc.perform(put("/shopping-list/" + itemId + "/mark-bought"))
                .andExpect(status().isOk());
    }

    @Test
    void updateShoppingItemAssigneesShouldReturnResponse() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(shoppingListService.updateShoppingItemAssignees(eq(itemId), anyList()))
                .thenReturn(ShoppingItemResponse.builder().build());

        mockMvc.perform(put("/shopping-list/" + itemId + "/assignees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of())))
                .andExpect(status().isOk());
    }

    @Test
    void deleteShoppingItemShouldReturnNoContent() throws Exception {
        UUID itemId = UUID.randomUUID();
        doNothing().when(shoppingListService).deleteShoppingItem(itemId);

        mockMvc.perform(delete("/shopping-list/" + itemId))
                .andExpect(status().isNoContent());
    }

    @Test
    void clearShoppingListShouldReturnCount() throws Exception {
        UUID houseId = UUID.randomUUID();
        when(shoppingListService.clearPendingList(houseId)).thenReturn(5);

        mockMvc.perform(delete("/shopping-list/" + houseId + "/clear"))
                .andExpect(status().isOk());
    }

    @Test
    void searchProductSuggestionsShouldReturnHttpStatusException() throws Exception {
        org.springframework.web.server.ResponseStatusException ex = 
                new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "High Demand");
        when(shoppingListService.searchProductSuggestions("leche", false)).thenThrow(ex);

        mockMvc.perform(get("/shopping-list/search").param("query", "leche"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void searchProductSuggestionsShouldReturn500OnGenericException() throws Exception {
        when(shoppingListService.searchProductSuggestions("leche", false)).thenThrow(new RuntimeException("Oops"));

        mockMvc.perform(get("/shopping-list/search").param("query", "leche"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getShoppingListShouldReturn500OnException() throws Exception {
        UUID houseId = UUID.randomUUID();
        when(shoppingListService.getShoppingList(houseId)).thenThrow(new RuntimeException("Oops"));

        mockMvc.perform(get("/shopping-list/" + houseId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getPendingItemsShouldReturn500OnException() throws Exception {
        UUID houseId = UUID.randomUUID();
        when(shoppingListService.getPendingItems(houseId)).thenThrow(new RuntimeException("Oops"));

        mockMvc.perform(get("/shopping-list/" + houseId + "/pending"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getBoughtItemsShouldReturn500OnException() throws Exception {
        UUID houseId = UUID.randomUUID();
        when(shoppingListService.getBoughtItems(houseId)).thenThrow(new RuntimeException("Oops"));

        mockMvc.perform(get("/shopping-list/" + houseId + "/bought"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getEstimatedBudgetShouldReturn500OnException() throws Exception {
        UUID houseId = UUID.randomUUID();
        when(shoppingListService.getEstimatedBudget(houseId)).thenThrow(new RuntimeException("Oops"));

        mockMvc.perform(get("/shopping-list/" + houseId + "/budget"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void addShoppingItemShouldReturnBadRequestOnIllegalArgumentException() throws Exception {
        AddShoppingItemRequest request = AddShoppingItemRequest.builder()
                .productName("Manzana")
                .houseId(UUID.randomUUID())
                .addedById(UUID.randomUUID())
                .build();
        when(shoppingListService.addShoppingItem(any(AddShoppingItemRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid data"));

        mockMvc.perform(post("/shopping-list/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addShoppingItemShouldReturn500OnGenericException() throws Exception {
        AddShoppingItemRequest request = AddShoppingItemRequest.builder()
                .productName("Manzana")
                .houseId(UUID.randomUUID())
                .addedById(UUID.randomUUID())
                .build();
        when(shoppingListService.addShoppingItem(any(AddShoppingItemRequest.class)))
                .thenThrow(new RuntimeException("Oops"));

        mockMvc.perform(post("/shopping-list/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void checkoutShoppingListShouldReturnBadRequestOnIllegalState() throws Exception {
        CheckoutRequest request = CheckoutRequest.builder()
                .houseId(UUID.randomUUID())
                .paidById(UUID.randomUUID())
                .totalRealAmount(new BigDecimal("15.00"))
                .build();
        doThrow(new IllegalStateException("Invalid state")).when(shoppingListService).checkoutShoppingList(any(CheckoutRequest.class));

        mockMvc.perform(post("/shopping-list/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkoutShoppingListShouldReturn500OnGenericException() throws Exception {
        CheckoutRequest request = CheckoutRequest.builder()
                .houseId(UUID.randomUUID())
                .paidById(UUID.randomUUID())
                .totalRealAmount(new BigDecimal("15.00"))
                .build();
        doThrow(new RuntimeException("Oops")).when(shoppingListService).checkoutShoppingList(any(CheckoutRequest.class));

        mockMvc.perform(post("/shopping-list/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void markAsBoughtShouldReturnNotFoundOnIllegalArgumentException() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(shoppingListService.markAsBought(itemId)).thenThrow(new IllegalArgumentException("Not found"));

        mockMvc.perform(put("/shopping-list/" + itemId + "/mark-bought"))
                .andExpect(status().isNotFound());
    }

    @Test
    void markAsBoughtShouldReturn500OnGenericException() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(shoppingListService.markAsBought(itemId)).thenThrow(new RuntimeException("Oops"));

        mockMvc.perform(put("/shopping-list/" + itemId + "/mark-bought"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void updateShoppingItemAssigneesShouldReturnNotFoundOnIllegalArgumentException() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(shoppingListService.updateShoppingItemAssignees(eq(itemId), anyList()))
                .thenThrow(new IllegalArgumentException("Not found"));

        mockMvc.perform(put("/shopping-list/" + itemId + "/assignees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of())))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateShoppingItemAssigneesShouldReturn500OnGenericException() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(shoppingListService.updateShoppingItemAssignees(eq(itemId), anyList()))
                .thenThrow(new RuntimeException("Oops"));

        mockMvc.perform(put("/shopping-list/" + itemId + "/assignees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of())))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void deleteShoppingItemShouldReturnNotFoundOnIllegalArgumentException() throws Exception {
        UUID itemId = UUID.randomUUID();
        doThrow(new IllegalArgumentException("Not found")).when(shoppingListService).deleteShoppingItem(itemId);

        mockMvc.perform(delete("/shopping-list/" + itemId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteShoppingItemShouldReturn500OnGenericException() throws Exception {
        UUID itemId = UUID.randomUUID();
        doThrow(new RuntimeException("Oops")).when(shoppingListService).deleteShoppingItem(itemId);

        mockMvc.perform(delete("/shopping-list/" + itemId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void clearShoppingListShouldReturn500OnException() throws Exception {
        UUID houseId = UUID.randomUUID();
        when(shoppingListService.clearPendingList(houseId)).thenThrow(new RuntimeException("Oops"));

        mockMvc.perform(delete("/shopping-list/" + houseId + "/clear"))
                .andExpect(status().isInternalServerError());
    }
}

