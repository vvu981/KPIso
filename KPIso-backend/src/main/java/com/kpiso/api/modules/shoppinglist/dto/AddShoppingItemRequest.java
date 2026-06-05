package com.kpiso.api.modules.shoppinglist.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddShoppingItemRequest {
    private UUID houseId;
    private UUID addedById;
    private String productName;
    private List<UUID> assignedUserIds;
    // Nuevo: permitir sobreescritura manual
    private Double manualPrice;
    private Boolean isManual;
    private Integer quantity;
}