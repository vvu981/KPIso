package com.kpiso.api.modules.expense.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseResponse {
    private UUID id;
    private String title;
    private BigDecimal amount;
    private UUID paidById;
    private String paidByUsername;
    private List<String> participantUsernames;
    private boolean settled;
    private LocalDateTime createdAt;
}