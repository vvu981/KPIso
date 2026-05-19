package com.kpiso.api.modules.shoppinglist.infrastructure.client;

import lombok.*;

/**
 * DTO que representa los detalles de un producto obtenidos desde el catálogo externo.
 * Proporciona una abstracción independiente del proveedor específico.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetails {

    /**
     * Nombre normalizado del producto
     */
    private String name;

    /**
     * URL de la imagen del producto
     */
    private String imageUrl;

    /**
     * Categoría principal del producto (ej: "dairies", "beverages", etc.)
     */
    private String mainCategory;

    /**
     * Tags de categorías (para estimación de precio)
     */
    private String categoryTags;

    /**
     * Indica si la búsqueda fue exitosa
     */
    private boolean found;

    /**
     * Mensaje de error (en caso de que found sea false)
     */
    private String errorMessage;

    /**
     * Factory method para crear un producto genérico cuando no se encuentra el producto
     */
    public static ProductDetails createNotFound(String query) {
        return ProductDetails.builder()
                .name(query)
                .found(false)
                .errorMessage("Producto no encontrado en el catálogo")
                .build();
    }
}
