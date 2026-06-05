package com.kpiso.api.modules.expense.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDirectPaymentRequest {

    @NotNull(message = "El emisor (senderId) es obligatorio")
    private UUID senderId;

    @NotNull(message = "El receptor (recipientId) es obligatorio")
    private UUID recipientId;

    @NotNull(message = "El importe es obligatorio")
    @Positive(message = "El importe debe ser mayor que cero")
    private BigDecimal amount;

    @NotNull(message = "El ID de la casa es obligatorio")
    private UUID houseId;
}
