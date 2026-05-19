package com.kpiso.api.modules.expense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExpenseRequest {

    @NotBlank(message = "El concepto del gasto es obligatorio")
    private String title;

    @NotNull(message = "El importe es obligatorio")
    @DecimalMin(value = "0.01", message = "El gasto debe ser mayor que cero")
    private BigDecimal amount;

    @NotNull(message = "La casa es obligatoria")
    private UUID houseId;

    @NotNull(message = "Debe indicarse quién pagó")
    private UUID paidById;

    @NotEmpty(message = "Debe haber al menos un participante en el gasto")
    private List<UUID> participantIds;

    // Opcional: si se provee, la división no será a partes iguales, sino exacta por usuario
    private java.util.Map<UUID, BigDecimal> exactSplits;
}