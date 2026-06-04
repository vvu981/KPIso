package com.kpiso.api.modules.shoppinglist.service;

import com.kpiso.api.modules.expense.ExpenseService;
import com.kpiso.api.modules.house.House;
import com.kpiso.api.modules.house.HouseMember;
import com.kpiso.api.modules.house.HouseMemberRepository;
import com.kpiso.api.modules.house.HouseRole;
import com.kpiso.api.modules.shoppinglist.ShoppingItem;
import com.kpiso.api.modules.shoppinglist.ShoppingItemRepository;
import com.kpiso.api.modules.shoppinglist.ShoppingItemStatus;
import com.kpiso.api.modules.shoppinglist.dto.*;
import com.kpiso.api.modules.shoppinglist.infrastructure.adapter.ScraperServiceAdapter;
import com.kpiso.api.modules.shoppinglist.infrastructure.client.ProductCatalogClient;
import com.kpiso.api.modules.shoppinglist.infrastructure.client.ProductDetails;
import com.kpiso.api.modules.user.User;
import com.kpiso.api.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShoppingListServiceTest {

    @Mock
    private ShoppingItemRepository shoppingItemRepository;

    @Mock
    private ProductCatalogClient productCatalogClient;

    @Mock
    private PriceEstimatorService priceEstimatorService;

    @Mock
    private ScraperServiceAdapter scraperServiceAdapter;

    @Mock
    private ExpenseService expenseService;

    @Mock
    private HouseMemberRepository houseMemberRepository;

    private ShoppingListService shoppingListService;

    private House house;
    private User user;

    @BeforeEach
    void setUp() {
        shoppingListService = new ShoppingListService(
                shoppingItemRepository,
                productCatalogClient,
                priceEstimatorService,
                scraperServiceAdapter,
                expenseService,
                houseMemberRepository
        );
        house = TestFixtures.house("Piso", "INV123");
        user = TestFixtures.user("user", "user@email.com");
    }

    @Test
    void searchProductSuggestionsShouldReturnSuggestions() {
        String query = "leche";
        ProductDetails details = ProductDetails.builder()
                .name("Leche Desnatada")
                .imageUrl("https://image.com")
                .categoryTags("dairy")
                .found(true)
                .build();
        when(scraperServiceAdapter.searchProductSuggestions(query, false)).thenReturn(List.of(details));
        when(priceEstimatorService.estimatePrice("dairy")).thenReturn(1.50);

        List<ProductSuggestionDto> suggestions = shoppingListService.searchProductSuggestions(query);

        assertEquals(1, suggestions.size());
        assertEquals("Leche Desnatada", suggestions.get(0).getName());
        assertEquals(1.50, suggestions.get(0).getEstimatedPrice());
    }

    @Test
    void addShoppingItemShouldFallbackOnCatalogError() {
        AddShoppingItemRequest request = AddShoppingItemRequest.builder()
                .houseId(house.getId())
                .addedById(user.getId())
                .productName("Manzana")
                .build();

        when(productCatalogClient.searchProduct("Manzana")).thenThrow(new RuntimeException("Catalog failed"));
        when(priceEstimatorService.estimatePrice(null)).thenReturn(0.80);
        when(shoppingItemRepository.save(any(ShoppingItem.class))).thenAnswer(invocation -> {
            ShoppingItem item = invocation.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });

        ShoppingItemResponse response = shoppingListService.addShoppingItem(request);

        assertEquals("Manzana", response.getName());
        assertEquals(0.80, response.getEstimatedPrice());
        assertNull(response.getImageUrl());
    }

    @Test
    void addShoppingItemShouldUseDetailsWhenProductFound() {
        AddShoppingItemRequest request = AddShoppingItemRequest.builder()
                .houseId(house.getId())
                .addedById(user.getId())
                .productName("Manzana")
                .manualPrice(1.20)
                .build();

        ProductDetails details = ProductDetails.builder()
                .name("Manzana Fuji")
                .imageUrl("https://manzana.jpg")
                .categoryTags("fruits")
                .found(true)
                .build();
        when(productCatalogClient.searchProduct("Manzana")).thenReturn(details);
        when(priceEstimatorService.estimatePrice("fruits")).thenReturn(0.90);
        when(shoppingItemRepository.save(any(ShoppingItem.class))).thenAnswer(invocation -> {
            ShoppingItem item = invocation.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });

        ShoppingItemResponse response = shoppingListService.addShoppingItem(request);

        assertEquals("Manzana Fuji", response.getName());
        assertEquals(1.20, response.getEstimatedPrice()); // manualPrice override
        assertEquals("https://manzana.jpg", response.getImageUrl());
    }

    @Test
    void getShoppingListShouldReturnCategorizedItems() {
        ShoppingItem pending = ShoppingItem.builder()
                .id(UUID.randomUUID())
                .houseId(house.getId())
                .name("Pan")
                .estimatedPrice(1.00)
                .status(ShoppingItemStatus.PENDING)
                .build();
        ShoppingItem bought = ShoppingItem.builder()
                .id(UUID.randomUUID())
                .houseId(house.getId())
                .name("Agua")
                .estimatedPrice(0.50)
                .status(ShoppingItemStatus.BOUGHT)
                .build();

        when(shoppingItemRepository.findByHouseIdAndStatusOrderByCreatedAtDesc(house.getId(), ShoppingItemStatus.PENDING))
                .thenReturn(List.of(pending));
        when(shoppingItemRepository.findByHouseIdAndStatusOrderByCreatedAtDesc(house.getId(), ShoppingItemStatus.BOUGHT))
                .thenReturn(List.of(bought));

        ShoppingListResponse list = shoppingListService.getShoppingList(house.getId());

        assertEquals(1, list.getPendingItems().size());
        assertEquals(1, list.getBoughtItems().size());
        assertEquals(1.00, list.getEstimatedBudget());
    }

    @Test
    void markAsBoughtShouldChangeStatus() {
        UUID itemId = UUID.randomUUID();
        ShoppingItem item = ShoppingItem.builder()
                .id(itemId)
                .name("Pan")
                .status(ShoppingItemStatus.PENDING)
                .build();

        when(shoppingItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(shoppingItemRepository.save(any(ShoppingItem.class))).thenAnswer(i -> i.getArgument(0));

        ShoppingItemResponse response = shoppingListService.markAsBought(itemId);

        assertEquals(ShoppingItemStatus.BOUGHT, response.getStatus());
    }

    @Test
    void markAsBoughtShouldFailWhenItemNotFound() {
        UUID itemId = UUID.randomUUID();
        when(shoppingItemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> shoppingListService.markAsBought(itemId));
    }

    @Test
    void updateShoppingItemAssigneesShouldSaveAssignees() {
        UUID itemId = UUID.randomUUID();
        ShoppingItem item = ShoppingItem.builder()
                .id(itemId)
                .name("Pan")
                .assignedUsers(new ArrayList<>())
                .build();
        List<UUID> assigneeIds = List.of(UUID.randomUUID());

        when(shoppingItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(shoppingItemRepository.save(any(ShoppingItem.class))).thenAnswer(i -> i.getArgument(0));

        ShoppingItemResponse response = shoppingListService.updateShoppingItemAssignees(itemId, assigneeIds);

        assertEquals(assigneeIds, response.getAssignedUserIds());
    }

    @Test
    void deleteShoppingItemShouldRemoveItem() {
        UUID itemId = UUID.randomUUID();
        ShoppingItem item = ShoppingItem.builder()
                .id(itemId)
                .name("Pan")
                .status(ShoppingItemStatus.PENDING)
                .build();

        when(shoppingItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        shoppingListService.deleteShoppingItem(itemId);

        verify(shoppingItemRepository).delete(item);
    }

    @Test
    void deleteShoppingItemShouldFailIfAlreadyBought() {
        UUID itemId = UUID.randomUUID();
        ShoppingItem item = ShoppingItem.builder()
                .id(itemId)
                .name("Pan")
                .status(ShoppingItemStatus.BOUGHT)
                .build();

        when(shoppingItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        assertThrows(IllegalStateException.class, () -> shoppingListService.deleteShoppingItem(itemId));
    }

    @Test
    void checkoutShoppingListShouldCreateExpenseAndMarkBought() {
        CheckoutRequest request = CheckoutRequest.builder()
                .houseId(house.getId())
                .paidById(user.getId())
                .totalRealAmount(new BigDecimal("10.00"))
                .build();

        ShoppingItem item1 = ShoppingItem.builder()
                .id(UUID.randomUUID())
                .name("Manzana")
                .estimatedPrice(2.00)
                .status(ShoppingItemStatus.PENDING)
                .build();
        ShoppingItem item2 = ShoppingItem.builder()
                .id(UUID.randomUUID())
                .name("Plátano")
                .estimatedPrice(3.00)
                .status(ShoppingItemStatus.PENDING)
                .build();

        when(shoppingItemRepository.findByHouseIdAndStatusOrderByCreatedAtDesc(house.getId(), ShoppingItemStatus.PENDING))
                .thenReturn(List.of(item1, item2));

        HouseMember member = TestFixtures.houseMember(house, user, HouseRole.ADMIN, "#111111");
        when(houseMemberRepository.findByHouseId(house.getId())).thenReturn(List.of(member));

        shoppingListService.checkoutShoppingList(request);

        assertEquals(ShoppingItemStatus.BOUGHT, item1.getStatus());
        assertEquals(ShoppingItemStatus.BOUGHT, item2.getStatus());
        assertNotNull(item1.getCheckoutId());
        verify(shoppingItemRepository).saveAll(any());
        verify(expenseService).createExpense(any());
    }

    @Test
    void checkoutShoppingListShouldFailWhenNoPendingItems() {
        CheckoutRequest request = CheckoutRequest.builder()
                .houseId(house.getId())
                .build();

        when(shoppingItemRepository.findByHouseIdAndStatusOrderByCreatedAtDesc(house.getId(), ShoppingItemStatus.PENDING))
                .thenReturn(Collections.emptyList());

        assertThrows(IllegalStateException.class, () -> shoppingListService.checkoutShoppingList(request));
    }

    @Test
    void getPendingItemsShouldReturnPendingOnly() {
        when(shoppingItemRepository.findByHouseIdAndStatusOrderByCreatedAtDesc(house.getId(), ShoppingItemStatus.PENDING))
                .thenReturn(List.of());
        List<ShoppingItemResponse> items = shoppingListService.getPendingItems(house.getId());
        assertTrue(items.isEmpty());
    }

    @Test
    void getBoughtItemsShouldReturnBoughtOnly() {
        when(shoppingItemRepository.findByHouseIdAndStatusOrderByCreatedAtDesc(house.getId(), ShoppingItemStatus.BOUGHT))
                .thenReturn(List.of());
        List<ShoppingItemResponse> items = shoppingListService.getBoughtItems(house.getId());
        assertTrue(items.isEmpty());
    }

    @Test
    void getEstimatedBudgetShouldSumPendingPrices() {
        ShoppingItem item = ShoppingItem.builder().estimatedPrice(4.50).build();
        when(shoppingItemRepository.findByHouseIdAndStatusOrderByCreatedAtDesc(house.getId(), ShoppingItemStatus.PENDING))
                .thenReturn(List.of(item));
        Double budget = shoppingListService.getEstimatedBudget(house.getId());
        assertEquals(4.50, budget);
    }
}
