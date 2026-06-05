package com.kpiso.api.modules.house.stats.dto;

import java.util.List;

public class ProductStatsDto {
    private List<ProductStatItem> topFrequentProducts;
    private List<ProductStatsDto.ProductStatItem> topExpensiveProducts;

    public ProductStatsDto() {}

    public ProductStatsDto(List<ProductStatItem> topFrequentProducts, List<ProductStatItem> topExpensiveProducts) {
        this.topFrequentProducts = topFrequentProducts;
        this.topExpensiveProducts = topExpensiveProducts;
    }

    public List<ProductStatItem> getTopFrequentProducts() { return topFrequentProducts; }
    public void setTopFrequentProducts(List<ProductStatItem> topFrequentProducts) { this.topFrequentProducts = topFrequentProducts; }
    public List<ProductStatItem> getTopExpensiveProducts() { return topExpensiveProducts; }
    public void setTopExpensiveProducts(List<ProductStatItem> topExpensiveProducts) { this.topExpensiveProducts = topExpensiveProducts; }

    public static class ProductStatItem {
        private String name;
        private Double unitPrice;

        public ProductStatItem() {}

        public ProductStatItem(String name, Double unitPrice) {
            this.name = name;
            this.unitPrice = unitPrice;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }
    }
}
