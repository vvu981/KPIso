package com.kpiso.api.modules.shoppinglist.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingListResponse {

    private UUID houseId;
    private List<ShoppingItemResponse> pendingItems;
    private List<ShoppingItemResponse> boughtItems;
    private Double estimatedBudget;
}
