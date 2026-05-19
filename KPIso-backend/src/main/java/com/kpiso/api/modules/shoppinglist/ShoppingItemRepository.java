package com.kpiso.api.modules.shoppinglist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, UUID> {

    /**
     * Obtiene todos los ítems de compra para una vivienda, ordenados por fecha de creación descendente
     */
    List<ShoppingItem> findByHouseIdOrderByCreatedAtDesc(UUID houseId);

    /**
     * Obtiene todos los ítems de compra en estado PENDING para una vivienda
     */
    List<ShoppingItem> findByHouseIdAndStatusOrderByCreatedAtDesc(UUID houseId, ShoppingItemStatus status);

    /**
     * Cuenta los ítems en estado PENDING para una vivienda (para validación)
     */
    long countByHouseIdAndStatus(UUID houseId, ShoppingItemStatus status);

    /**
     * Elimina todos los ítems asociados a una vivienda (para limpieza de datos)
     */
    @Query("DELETE FROM ShoppingItem si WHERE si.houseId = :houseId")
    void deleteAllByHouseId(@Param("houseId") UUID houseId);
}
