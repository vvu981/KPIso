package com.kpiso.api.modules.shoppinglist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddShoppingItemRequest {

    @NotBlank(message = "El nombre del producto no puede estar vacío")
    private String productName;

    @NotNull(message = "El ID de la vivienda es requerido")
    private UUID houseId;

    @NotNull(message = "El ID del usuario que añade es requerido")
    private UUID addedById;

    private java.util.List<UUID> assignedUserIds;
}
