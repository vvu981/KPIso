package com.kpiso.api.modules.shoppinglist;

import com.kpiso.api.modules.shoppinglist.dto.AddShoppingItemRequest;
import com.kpiso.api.modules.shoppinglist.dto.ProductSuggestionDto;
import com.kpiso.api.modules.shoppinglist.dto.ShoppingItemResponse;
import com.kpiso.api.modules.shoppinglist.dto.ShoppingListResponse;
import com.kpiso.api.modules.shoppinglist.service.ShoppingListService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST para la gestión de listas de compra.
 * 
 * Endpoints disponibles:
 * - GET    /shopping-list/search                 → Busca sugerencias de productos
 * - GET    /shopping-list/{houseId}              → Obtiene la lista completa
 * - GET    /shopping-list/{houseId}/pending      → Obtiene solo ítems pendientes
 * - GET    /shopping-list/{houseId}/bought       → Obtiene solo ítems comprados
 * - POST   /shopping-list/add                    → Añade un nuevo producto
 * - PUT    /shopping-list/{itemId}/mark-bought   → Marca como comprado
 * - DELETE /shopping-list/{itemId}               → Elimina un ítem
 */
@Slf4j
@RestController
@RequestMapping("/shopping-list")
public class ShoppingListController {

    private final ShoppingListService shoppingListService;

    public ShoppingListController(ShoppingListService shoppingListService) {
        this.shoppingListService = shoppingListService;
    }

    /**
     * Busca sugerencias de productos basándose en el término de búsqueda.
     * Devuelve una lista de productos con nombre, imagen, categoría y precio estimado.
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchProductSuggestions(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "false") boolean useFallback) {
        log.info("GET /shopping-list/search - Buscando sugerencias para: {} (useFallback: {})", query, useFallback);
        try {
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            List<ProductSuggestionDto> suggestions = shoppingListService.searchProductSuggestions(query.trim(), useFallback);
            return ResponseEntity.ok(suggestions);
        } catch (ResponseStatusException e) {
            log.warn("Error de estado HTTP al buscar sugerencias: {} - {}", e.getStatusCode(), e.getReason());
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (Exception e) {
            log.error("Error al buscar sugerencias de productos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar la búsqueda");
        }
    }

    /**
     * Obtiene la lista de compra completa de una vivienda.
     * Agrupa ítems PENDING y BOUGHT, calcula presupuesto estimado.
     */
    @GetMapping("/{houseId}")
    public ResponseEntity<ShoppingListResponse> getShoppingList(@PathVariable UUID houseId) {
        log.info("GET /shopping-list/{} - Obteniendo lista de compra", houseId);
        try {
            ShoppingListResponse response = shoppingListService.getShoppingList(houseId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al obtener lista de compra para house: {}", houseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene solo los ítems en estado PENDING de una vivienda.
     */
    @GetMapping("/{houseId}/pending")
    public ResponseEntity<List<ShoppingItemResponse>> getPendingItems(@PathVariable UUID houseId) {
        log.info("GET /shopping-list/{}/pending - Obteniendo ítems pendientes", houseId);
        try {
            List<ShoppingItemResponse> items = shoppingListService.getPendingItems(houseId);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            log.error("Error al obtener ítems pendientes para house: {}", houseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene solo los ítems en estado BOUGHT de una vivienda.
     */
    @GetMapping("/{houseId}/bought")
    public ResponseEntity<List<ShoppingItemResponse>> getBoughtItems(@PathVariable UUID houseId) {
        log.info("GET /shopping-list/{}/bought - Obteniendo ítems comprados", houseId);
        try {
            List<ShoppingItemResponse> items = shoppingListService.getBoughtItems(houseId);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            log.error("Error al obtener ítems comprados para house: {}", houseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene el presupuesto estimado total de ítems PENDING.
     */
    @GetMapping("/{houseId}/budget")
    public ResponseEntity<Double> getEstimatedBudget(@PathVariable UUID houseId) {
        log.info("GET /shopping-list/{}/budget - Obteniendo presupuesto estimado", houseId);
        try {
            Double budget = shoppingListService.getEstimatedBudget(houseId);
            return ResponseEntity.ok(budget);
        } catch (Exception e) {
            log.error("Error al calcular presupuesto para house: {}", houseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Añade un nuevo producto a la lista de compra.
     * 
     * Proceso:
     * 1. Valida la solicitud
     * 2. Consulta Open Food Facts
     * 3. Estima el precio
     * 4. Persiste en BD
     */
    @PostMapping("/add")
    public ResponseEntity<?> addShoppingItem(@Valid @RequestBody AddShoppingItemRequest request) {
        log.info("POST /shopping-list/add - Añadiendo producto: {} para house: {}", 
                request.getProductName(), request.getHouseId());
        try {
            ShoppingItemResponse item = shoppingListService.addShoppingItem(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(item);
        } catch (IllegalArgumentException e) {
            log.warn("Validación fallida al añadir ítem: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error al añadir ítem a la lista de compra", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Finaliza la lista de compra (Checkout), calcula saldos y genera el gasto.
     */
    @PostMapping("/checkout")
    public ResponseEntity<?> checkoutShoppingList(@Valid @RequestBody com.kpiso.api.modules.shoppinglist.dto.CheckoutRequest request) {
        log.info("POST /shopping-list/checkout - Checkout para house: {} por importe: {}", 
                request.getHouseId(), request.getTotalRealAmount());
        try {
            shoppingListService.checkoutShoppingList(request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Error de negocio en checkout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado en checkout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Marca un ítem como comprado (cambia estado a BOUGHT).
     */
    @PutMapping("/{itemId}/mark-bought")
    public ResponseEntity<?> markAsBought(@PathVariable UUID itemId) {
        log.info("PUT /shopping-list/{}/mark-bought - Marcando como comprado", itemId);
        try {
            ShoppingItemResponse item = shoppingListService.markAsBought(itemId);
            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException e) {
            log.warn("Ítem no encontrado: {}", itemId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error al marcar ítem como comprado: {}", itemId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Actualiza los convivientes asignados a un ítem de la lista.
     */
    @PutMapping("/{itemId}/assignees")
    public ResponseEntity<?> updateShoppingItemAssignees(@PathVariable UUID itemId, @RequestBody List<UUID> assigneeIds) {
        log.info("PUT /shopping-list/{}/assignees - Actualizando convivientes asignados", itemId);
        try {
            ShoppingItemResponse item = shoppingListService.updateShoppingItemAssignees(itemId, assigneeIds);
            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException e) {
            log.warn("Ítem no encontrado: {}", itemId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error al actualizar asignaciones del ítem: {}", itemId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Elimina un ítem de la lista de compra.
     */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<?> deleteShoppingItem(@PathVariable UUID itemId) {
        log.info("DELETE /shopping-list/{} - Eliminando ítem", itemId);
        try {
            shoppingListService.deleteShoppingItem(itemId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Ítem no encontrado: {}", itemId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error al eliminar ítem de la lista de compra: {}", itemId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    /**
     * Elimina todos los ítems PENDING de una vivienda (limpiar lista de compra).
     */
    @DeleteMapping("/{houseId}/clear")
    public ResponseEntity<?> clearShoppingList(@PathVariable UUID houseId) {
        log.info("DELETE /shopping-list/{}/clear - Limpiando lista de compra", houseId);
        try {
            int deleted = shoppingListService.clearPendingList(houseId);
            return ResponseEntity.ok(Map.of("deleted", deleted));
        } catch (Exception e) {
            log.error("Error al limpiar la lista de compra para house: {}", houseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
