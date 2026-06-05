package com.kpiso.api.modules.house.stats.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ExpenseDto {
    private UUID id;
    private String description;
    private BigDecimal amount;
    private java.time.LocalDate date;
    private UUID memberId;

    public ExpenseDto() {}

    public ExpenseDto(UUID id, String description, BigDecimal amount, java.time.LocalDate date, UUID memberId) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.memberId = memberId;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public java.time.LocalDate getDate() { return date; }
    public void setDate(java.time.LocalDate date) { this.date = date; }
    public UUID getMemberId() { return memberId; }
    public void setMemberId(UUID memberId) { this.memberId = memberId; }
}
