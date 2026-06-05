package com.kpiso.api.modules.shoppinglist.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequest {

    @NotNull(message = "El ID de la casa es obligatorio")
    private UUID houseId;

    @NotNull(message = "Debe indicarse el usuario que pagó la compra")
    private UUID paidById;

    @NotNull(message = "El importe real total es obligatorio")
    @DecimalMin(value = "0.01", message = "El importe real debe ser mayor que cero")
    private BigDecimal totalRealAmount;
}
