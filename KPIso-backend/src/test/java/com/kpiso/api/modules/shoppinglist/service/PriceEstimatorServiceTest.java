package com.kpiso.api.modules.shoppinglist.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PriceEstimatorServiceTest {

    private final PriceEstimatorService priceEstimatorService = new PriceEstimatorService();

    private void checkPriceRange(Double price, double basePrice) {
        assertNotNull(price);
        double minPrice = basePrice - 0.25;
        double maxPrice = basePrice + 0.25;
        assertTrue(price >= minPrice && price <= maxPrice, 
                "Price " + price + " was out of range [" + minPrice + ", " + maxPrice + "]");
    }

    @Test
    void estimatePriceShouldHandleNullOrBlank() {
        checkPriceRange(priceEstimatorService.estimatePrice(null), 2.50);
        checkPriceRange(priceEstimatorService.estimatePrice(""), 2.50);
        checkPriceRange(priceEstimatorService.estimatePrice("   "), 2.50);
    }

    @Test
    void estimatePriceShouldDetectDairies() {
        checkPriceRange(priceEstimatorService.estimatePrice("dairies"), 1.20);
        checkPriceRange(priceEstimatorService.estimatePrice("dairy"), 1.20);
        checkPriceRange(priceEstimatorService.estimatePrice("milk"), 1.20);
    }

    @Test
    void estimatePriceShouldDetectBeverages() {
        checkPriceRange(priceEstimatorService.estimatePrice("beverages"), 1.50);
        checkPriceRange(priceEstimatorService.estimatePrice("drinks"), 1.50);
        checkPriceRange(priceEstimatorService.estimatePrice("water"), 1.50);
    }

    @Test
    void estimatePriceShouldDetectProduce() {
        checkPriceRange(priceEstimatorService.estimatePrice("fruits"), 2.80);
        checkPriceRange(priceEstimatorService.estimatePrice("vegetables"), 2.80);
        checkPriceRange(priceEstimatorService.estimatePrice("produce"), 2.80);
        checkPriceRange(priceEstimatorService.estimatePrice("fresh"), 2.80);
    }

    @Test
    void estimatePriceShouldDetectMeats() {
        checkPriceRange(priceEstimatorService.estimatePrice("meats"), 8.50);
        checkPriceRange(priceEstimatorService.estimatePrice("meat"), 8.50);
        checkPriceRange(priceEstimatorService.estimatePrice("fish"), 8.50);
        checkPriceRange(priceEstimatorService.estimatePrice("seafood"), 8.50);
    }

    @Test
    void estimatePriceShouldDetectBakery() {
        checkPriceRange(priceEstimatorService.estimatePrice("breads"), 1.80);
        checkPriceRange(priceEstimatorService.estimatePrice("bread"), 1.80);
        checkPriceRange(priceEstimatorService.estimatePrice("bakery"), 1.80);
        checkPriceRange(priceEstimatorService.estimatePrice("bakeries"), 1.80);
    }

    @Test
    void estimatePriceShouldDetectGrains() {
        checkPriceRange(priceEstimatorService.estimatePrice("cereals"), 1.30);
        checkPriceRange(priceEstimatorService.estimatePrice("grains"), 1.30);
        checkPriceRange(priceEstimatorService.estimatePrice("pasta"), 1.30);
        checkPriceRange(priceEstimatorService.estimatePrice("rice"), 1.30);
    }

    @Test
    void estimatePriceShouldDetectSeasonings() {
        checkPriceRange(priceEstimatorService.estimatePrice("seasonings"), 3.50);
        checkPriceRange(priceEstimatorService.estimatePrice("spices"), 3.50);
        checkPriceRange(priceEstimatorService.estimatePrice("condiments"), 3.50);
    }

    @Test
    void estimatePriceShouldDetectSnacks() {
        checkPriceRange(priceEstimatorService.estimatePrice("snacks"), 2.30);
        checkPriceRange(priceEstimatorService.estimatePrice("cookies"), 2.30);
        checkPriceRange(priceEstimatorService.estimatePrice("chips"), 2.30);
    }

    @Test
    void estimatePriceShouldDetectFrozen() {
        checkPriceRange(priceEstimatorService.estimatePrice("frozen"), 4.20);
        checkPriceRange(priceEstimatorService.estimatePrice("freezer"), 4.20);
    }

    @Test
    void estimatePriceShouldDetectCleaning() {
        checkPriceRange(priceEstimatorService.estimatePrice("cleaning"), 2.90);
        checkPriceRange(priceEstimatorService.estimatePrice("detergents"), 2.90);
        checkPriceRange(priceEstimatorService.estimatePrice("hygiene"), 2.90);
    }

    @Test
    void estimatePriceShouldFallbackToDefault() {
        checkPriceRange(priceEstimatorService.estimatePrice("unknown-tag"), 2.50);
    }
}
