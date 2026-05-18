package com.kpiso.api.modules.expense.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberStatusResponse {
    private BigDecimal balance;
    private String color;
    private Integer points; // Puntos KPI acumulados por el usuario
}