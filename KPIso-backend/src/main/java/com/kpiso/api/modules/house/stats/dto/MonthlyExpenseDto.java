package com.kpiso.api.modules.house.stats.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public class MonthlyExpenseDto {
    private YearMonth yearMonth;
    private BigDecimal totalAmount;

    public MonthlyExpenseDto() {}

    public MonthlyExpenseDto(YearMonth yearMonth, BigDecimal totalAmount) {
        this.yearMonth = yearMonth;
        this.totalAmount = totalAmount;
    }

    public YearMonth getYearMonth() { return yearMonth; }
    public void setYearMonth(YearMonth yearMonth) { this.yearMonth = yearMonth; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
