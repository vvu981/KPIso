package com.kpiso.api.modules.shoppinglist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CachedProductRepository extends JpaRepository<CachedProduct, UUID> {
    
    /**
     * Busca las primeras 8 coincidencias por nombre ignorando mayúsculas/minúsculas.
     */
    List<CachedProduct> findTop8ByNameContainingIgnoreCase(String name);

    /**
     * Comprueba si ya existe un producto con el nombre exacto ignorando mayúsculas/minúsculas.
     */
    boolean existsByNameIgnoreCase(String name);
}
