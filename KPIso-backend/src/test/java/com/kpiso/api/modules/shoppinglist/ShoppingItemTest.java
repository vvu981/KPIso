package com.kpiso.api.modules.shoppinglist;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ShoppingItemTest {

    @Test
    void testShoppingItemGettersSettersAndBuilder() {
        UUID id = UUID.randomUUID();
        UUID houseId = UUID.randomUUID();
        UUID addedById = UUID.randomUUID();
        UUID checkoutId = UUID.randomUUID();
        List<UUID> assigned = List.of(UUID.randomUUID());
        LocalDateTime now = LocalDateTime.now();

        ShoppingItem item = ShoppingItem.builder()
                .id(id)
                .houseId(houseId)
                .addedById(addedById)
                .name("Manzana")
                .estimatedPrice(1.50)
                .imageUrl("http://img.png")
                .status(ShoppingItemStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .checkoutId(checkoutId)
                .assignedUsers(assigned)
                .build();

        assertEquals(id, item.getId());
        assertEquals(houseId, item.getHouseId());
        assertEquals(addedById, item.getAddedById());
        assertEquals("Manzana", item.getName());
        assertEquals(1.50, item.getEstimatedPrice());
        assertEquals("http://img.png", item.getImageUrl());
        assertEquals(ShoppingItemStatus.PENDING, item.getStatus());
        assertEquals(now, item.getCreatedAt());
        assertEquals(now, item.getUpdatedAt());
        assertEquals(checkoutId, item.getCheckoutId());
        assertEquals(assigned, item.getAssignedUsers());

        // Test setters
        item.setName("Pera");
        assertEquals("Pera", item.getName());

        // Test NoArgsConstructor
        ShoppingItem empty = new ShoppingItem();
        assertNull(empty.getId());
    }

    @Test
    void testLifecycleCallbacks() {
        ShoppingItem item = new ShoppingItem();
        assertNull(item.getCreatedAt());
        assertNull(item.getUpdatedAt());

        item.onCreate();
        assertNotNull(item.getCreatedAt());
        assertNotNull(item.getUpdatedAt());

        LocalDateTime created = item.getCreatedAt();
        item.onUpdate();
        assertNotNull(item.getUpdatedAt());
    }
}
