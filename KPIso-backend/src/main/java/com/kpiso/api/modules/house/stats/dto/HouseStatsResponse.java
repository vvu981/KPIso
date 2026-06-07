package com.kpiso.api.modules.house.stats.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HouseStatsResponse {
    private Map<UUID, BigDecimal> livingCostPerMember;
    private List<MonthlyExpenseDto> monthlyExpenseEvolution;
    private List<ExpenseDto> topExpenses;
    private ProductStatsDto productStats;
    private Map<UUID, Integer> taskKpiPoints;
    private List<Map<String, Object>> monthlyKpiEvolution;
    private Map<UUID, Integer> assignedKpiPoints;

    // Getters and Setters
    public Map<UUID, BigDecimal> getLivingCostPerMember() { return livingCostPerMember; }
    public void setLivingCostPerMember(Map<UUID, BigDecimal> livingCostPerMember) { this.livingCostPerMember = livingCostPerMember; }
    public List<MonthlyExpenseDto> getMonthlyExpenseEvolution() { return monthlyExpenseEvolution; }
    public void setMonthlyExpenseEvolution(List<MonthlyExpenseDto> monthlyExpenseEvolution) { this.monthlyExpenseEvolution = monthlyExpenseEvolution; }
    public List<ExpenseDto> getTopExpenses() { return topExpenses; }
    public void setTopExpenses(List<ExpenseDto> topExpenses) { this.topExpenses = topExpenses; }
    public ProductStatsDto getProductStats() { return productStats; }
    public void setProductStats(ProductStatsDto productStats) { this.productStats = productStats; }
    public Map<UUID, Integer> getTaskKpiPoints() { return taskKpiPoints; }
    public void setTaskKpiPoints(Map<UUID, Integer> taskKpiPoints) { this.taskKpiPoints = taskKpiPoints; }
    public List<Map<String, Object>> getMonthlyKpiEvolution() { return monthlyKpiEvolution; }
    public void setMonthlyKpiEvolution(List<Map<String, Object>> monthlyKpiEvolution) { this.monthlyKpiEvolution = monthlyKpiEvolution; }
    public Map<UUID, Integer> getAssignedKpiPoints() { return assignedKpiPoints; }
    public void setAssignedKpiPoints(Map<UUID, Integer> assignedKpiPoints) { this.assignedKpiPoints = assignedKpiPoints; }
}
