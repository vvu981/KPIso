package com.kpiso.api.modules.shoppinglist.dto;

import com.kpiso.api.modules.shoppinglist.ShoppingItemStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingItemResponse {

    private UUID id;
    private UUID houseId;
    private UUID addedById;
    private String name;
    private Double estimatedPrice;
    private String imageUrl;
    private ShoppingItemStatus status;
    private LocalDateTime createdAt;
    private java.util.List<UUID> assignedUserIds;
    private LocalDateTime updatedAt;
    private UUID checkoutId;
    private Integer quantity;
}
