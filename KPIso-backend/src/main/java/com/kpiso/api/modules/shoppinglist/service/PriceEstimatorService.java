package com.kpiso.api.modules.shoppinglist.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Servicio de estimación dinámica de precios basado en categorías de productos.
 * 
 * Proporciona precios estimados realistas según la categoría del producto.
 * Implementa la Responsabilidad Única (SOLID): solo calcula precios.
 */
@Slf4j
@Service
public class PriceEstimatorService {

    private static final double DEFAULT_PRICE = 2.50;
    private static final double PRICE_VARIATION = 0.50;

    private final Random random = new Random();

    /**
     * Estima el precio de un producto basándose en sus tags de categoría.
     *
     * @param categoryTags Tags de categoría delimitados por comas (ej: "dairies,en:milk")
     * @return Precio estimado en euros
     */
    public Double estimatePrice(String categoryTags) {
        if (categoryTags == null || categoryTags.isBlank()) {
            return getRandomizedPrice(DEFAULT_PRICE);
        }

        String lowerCaseTags = categoryTags.toLowerCase();

        // Categorías de lácteos
        if (lowerCaseTags.contains("dairies") || lowerCaseTags.contains("dairy") || lowerCaseTags.contains("milk")) {
            return getRandomizedPrice(1.20);
        }

        // Categorías de bebidas
        if (lowerCaseTags.contains("beverages") || lowerCaseTags.contains("drinks") || lowerCaseTags.contains("water")) {
            return getRandomizedPrice(1.50);
        }

        // Categorías de frutas y verduras
        if (lowerCaseTags.contains("fruits") || lowerCaseTags.contains("vegetables") || 
            lowerCaseTags.contains("produce") || lowerCaseTags.contains("fresh")) {
            return getRandomizedPrice(2.80);
        }

        // Categorías de carne y pescado
        if (lowerCaseTags.contains("meats") || lowerCaseTags.contains("meat") || 
            lowerCaseTags.contains("fish") || lowerCaseTags.contains("seafood")) {
            return getRandomizedPrice(8.50);
        }

        // Categorías de panadería
        if (lowerCaseTags.contains("breads") || lowerCaseTags.contains("bread") || 
            lowerCaseTags.contains("bakery") || lowerCaseTags.contains("bakeries")) {
            return getRandomizedPrice(1.80);
        }

        // Categorías de cereales y granos
        if (lowerCaseTags.contains("cereals") || lowerCaseTags.contains("grains") || 
            lowerCaseTags.contains("pasta") || lowerCaseTags.contains("rice")) {
            return getRandomizedPrice(1.30);
        }

        // Categorías de condimentos y especias
        if (lowerCaseTags.contains("seasonings") || lowerCaseTags.contains("spices") || 
            lowerCaseTags.contains("condiments")) {
            return getRandomizedPrice(3.50);
        }

        // Categorías de snacks
        if (lowerCaseTags.contains("snacks") || lowerCaseTags.contains("cookies") || 
            lowerCaseTags.contains("chips")) {
            return getRandomizedPrice(2.30);
        }

        // Categorías de alimentos congelados
        if (lowerCaseTags.contains("frozen") || lowerCaseTags.contains("freezer")) {
            return getRandomizedPrice(4.20);
        }

        // Categorías de productos de limpieza
        if (lowerCaseTags.contains("cleaning") || lowerCaseTags.contains("detergents") || 
            lowerCaseTags.contains("hygiene")) {
            return getRandomizedPrice(2.90);
        }

        // Precio genérico por defecto
        log.debug("Categoría desconocida: {}. Aplicando precio por defecto", categoryTags);
        return getRandomizedPrice(DEFAULT_PRICE);
    }

    /**
     * Añade una pequeña variación aleatoria al precio para mayor realismo.
     *
     * @param basePrice Precio base
     * @return Precio con variación aleatoria
     */
    private Double getRandomizedPrice(double basePrice) {
        double variation = (random.nextDouble() - 0.5) * PRICE_VARIATION;
        double finalPrice = basePrice + variation;
        // Redondear a 2 decimales
        return Math.round(finalPrice * 100.0) / 100.0;
    }
}
