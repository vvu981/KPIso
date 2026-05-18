package com.kpiso.api.modules.expense.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtSettlementResponse {
    private UUID debtorId;
    private String debtorUsername;
    private UUID creditorId;
    private String creditorUsername;
    private BigDecimal amount;
}