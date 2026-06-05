package com.kpiso.api.modules.expense.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectPaymentResponse {
    private UUID id;
    private UUID senderId;
    private String senderUsername;
    private UUID recipientId;
    private String recipientUsername;
    private BigDecimal amount;
    private UUID houseId;
    private boolean settled;
    private LocalDateTime createdAt;
}
