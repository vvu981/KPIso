package com.kpiso.api.modules.shoppinglist.infrastructure.client;

/**
 * Abstracción para interactuar con catálogos de productos externos.
 * Implementa el principio de Inversión de Dependencias (SOLID).
 * 
 * Permite cambiar el proveedor del catálogo sin afectar la lógica de negocio.
 */
public interface ProductCatalogClient {

    /**
     * Busca un producto en el catálogo externo.
     *
     * @param query Nombre o término de búsqueda del producto
     * @return Detalles del producto encontrado, o un resultado "no encontrado"
     */
    ProductDetails searchProduct(String query);
}
