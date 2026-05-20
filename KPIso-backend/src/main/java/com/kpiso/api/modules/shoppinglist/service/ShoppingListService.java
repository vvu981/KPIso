package com.kpiso.api.modules.shoppinglist.service;

import com.kpiso.api.modules.shoppinglist.ShoppingItem;
import com.kpiso.api.modules.shoppinglist.ShoppingItemRepository;
import com.kpiso.api.modules.shoppinglist.ShoppingItemStatus;
import com.kpiso.api.modules.shoppinglist.CachedProduct;
import com.kpiso.api.modules.shoppinglist.CachedProductRepository;
import com.kpiso.api.modules.shoppinglist.dto.AddShoppingItemRequest;
import com.kpiso.api.modules.shoppinglist.dto.ProductSuggestionDto;
import com.kpiso.api.modules.shoppinglist.dto.ShoppingItemResponse;
import com.kpiso.api.modules.shoppinglist.dto.ShoppingListResponse;
import com.kpiso.api.modules.shoppinglist.infrastructure.adapter.OpenFoodFactsAdapter;
import com.kpiso.api.modules.shoppinglist.infrastructure.client.ProductCatalogClient;
import com.kpiso.api.modules.shoppinglist.infrastructure.client.ProductDetails;
import com.kpiso.api.modules.expense.ExpenseService;
import com.kpiso.api.modules.expense.dto.CreateExpenseRequest;
import com.kpiso.api.modules.shoppinglist.dto.CheckoutRequest;
import com.kpiso.api.modules.house.HouseMemberRepository;
import com.kpiso.api.modules.house.HouseMember;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio de orquestación para la Lista de la Compra.
 * * Implementa la lógica de negocio coordinando:
 * - Consultas al catálogo de productos (ProductCatalogClient)
 * - Estimación de precios (PriceEstimatorService)
 * - Persistencia de datos (ShoppingItemRepository)
 * - Caché local de productos (CachedProductRepository)
 * * Sigue el principio de Responsabilidad Única (SOLID).
 */
@Slf4j
@Service
public class ShoppingListService {

    private final ShoppingItemRepository shoppingItemRepository;
    private final ProductCatalogClient productCatalogClient;
    private final PriceEstimatorService priceEstimatorService;
    private final OpenFoodFactsAdapter openFoodFactsAdapter;
    private final CachedProductRepository cachedProductRepository;
    private final ExpenseService expenseService;
    private final HouseMemberRepository houseMemberRepository;

    public ShoppingListService(ShoppingItemRepository shoppingItemRepository,
            ProductCatalogClient productCatalogClient,
            PriceEstimatorService priceEstimatorService,
            OpenFoodFactsAdapter openFoodFactsAdapter,
            CachedProductRepository cachedProductRepository,
            ExpenseService expenseService,
            HouseMemberRepository houseMemberRepository) {
        this.shoppingItemRepository = shoppingItemRepository;
        this.productCatalogClient = productCatalogClient;
        this.priceEstimatorService = priceEstimatorService;
        this.openFoodFactsAdapter = openFoodFactsAdapter;
        this.cachedProductRepository = cachedProductRepository;
        this.expenseService = expenseService;
        this.houseMemberRepository = houseMemberRepository;
    }

    /**
     * Busca sugerencias de productos basándose en el término de búsqueda.
     * Implementa un patrón de caché híbrido (Read-Through Cache) para optimizar el
     * rendimiento.
     */
    @Transactional
    public List<ProductSuggestionDto> searchProductSuggestions(String query) {
        log.debug("Buscando sugerencias de productos para: {}", query);

        // 1. Intentar buscar en la caché local de base de datos
        List<CachedProduct> localProducts = cachedProductRepository.findTop8ByNameContainingIgnoreCase(query);

        // Si tenemos suficientes sugerencias locales (8), las devolvemos inmediatamente
        // (Cache Hit)
        if (localProducts.size() >= 8) {
            log.debug("Cache Hit: Se encontraron sugerencias suficientes locales ({} productos)", localProducts.size());
            return localProducts.stream()
                    .map(product -> ProductSuggestionDto.builder()
                            .name(product.getName())
                            .imageUrl(product.getImageUrl())
                            .categoryTags(product.getCategoryTags())
                            .estimatedPrice(priceEstimatorService.estimatePrice(product.getCategoryTags()))
                            .build())
                    .collect(Collectors.toList());
        }

        log.debug("Cache Miss parcial: Se encontraron {} productos locales, consultando Open Food Facts",
                localProducts.size());

        // 2. Si no hay suficientes locales, consultar a la API externa
        List<ProductDetails> apiSuggestions = openFoodFactsAdapter.searchProductSuggestions(query);

        // 3. Persistir en caché los productos devueltos por la API que no existan
        // localmente
        for (ProductDetails apiProduct : apiSuggestions) {
            if (apiProduct.isFound() && apiProduct.getName() != null && !apiProduct.getName().isBlank()) {
                if (!cachedProductRepository.existsByNameIgnoreCase(apiProduct.getName().trim())) {
                    CachedProduct newCache = CachedProduct.builder()
                            .name(apiProduct.getName().trim())
                            .imageUrl(apiProduct.getImageUrl())
                            .mainCategory(apiProduct.getMainCategory())
                            .categoryTags(apiProduct.getCategoryTags())
                            .build();
                    try {
                        cachedProductRepository.save(newCache);
                        log.debug("Producto guardado en caché de base de datos: {}", newCache.getName());
                    } catch (Exception e) {
                        log.warn("No se pudo cachear el producto '{}' debido a un error: {}", newCache.getName(),
                                e.getMessage());
                    }
                }
            }
        }

        // 4. Volver a consultar la base de datos para obtener el listado consolidado y
        // actualizado (máximo 8)
        List<CachedProduct> consolidatedProducts = cachedProductRepository.findTop8ByNameContainingIgnoreCase(query);

        return consolidatedProducts.stream()
                .map(product -> ProductSuggestionDto.builder()
                        .name(product.getName())
                        .imageUrl(product.getImageUrl())
                        .categoryTags(product.getCategoryTags())
                        .estimatedPrice(priceEstimatorService.estimatePrice(product.getCategoryTags()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Añade un nuevo producto a la lista de compra.
     * * Proceso:
     * 1. Consulta Open Food Facts para obtener detalles normalizados del producto
     * (aislado para no bloquear)
     * 2. Estima el precio basándose en la categoría
     * 3. Persiste el ítem en la base de datos
     * 4. Almacena en caché local para enriquecer las futuras búsquedas de
     * autocompletado
     */
    @Transactional
    public ShoppingItemResponse addShoppingItem(AddShoppingItemRequest request) {
        log.info("Añadiendo ítem a la lista de compra para house: {} con producto: {}",
                request.getHouseId(), request.getProductName());

        // Aislar la llamada externa. Si la API falla, no impedimos que el usuario añada
        // el producto manualmente.
        ProductDetails productDetails = null;
        try {
            productDetails = productCatalogClient.searchProduct(request.getProductName());
        } catch (Exception e) {
            log.warn("El catálogo externo falló al buscar '{}'. Se registrará como entrada manual. Error: {}",
                    request.getProductName(), e.getMessage());
        }

        String productName = request.getProductName();
        String imageUrl = null;
        String categoryTags = null;

        // Si se encontró el producto y no hubo fallos de red, usar sus detalles
        // normalizados
        if (productDetails != null && productDetails.isFound()) {
            productName = productDetails.getName();
            imageUrl = productDetails.getImageUrl();
            categoryTags = productDetails.getCategoryTags();
            log.debug("Producto encontrado en Open Food Facts: {}", productName);

            // Auto-cachear el producto para búsquedas futuras si no existe
            if (productName != null && !productName.isBlank()
                    && !cachedProductRepository.existsByNameIgnoreCase(productName.trim())) {
                try {
                    CachedProduct newCache = CachedProduct.builder()
                            .name(productName.trim())
                            .imageUrl(imageUrl)
                            .mainCategory(productDetails.getMainCategory())
                            .categoryTags(categoryTags)
                            .build();
                    cachedProductRepository.save(newCache);
                    log.debug("Auto-cacheado del producto añadido: {}", newCache.getName());
                } catch (Exception e) {
                    log.warn("No se pudo auto-cachear el producto '{}': {}", productName, e.getMessage());
                }
            }
        } else {
            log.debug("Producto no encontrado o fallo en el sistema externo, usando entrada manual");
        }

        // Estimar precio basándose en la categoría
        Double estimatedPrice = priceEstimatorService.estimatePrice(categoryTags);

        // Permitir sobreescritura manual desde la petición: si viene un manualPrice
        // válido (>0), usarlo.
        Double finalPrice = estimatedPrice;
        if (request.getManualPrice() != null && request.getManualPrice() > 0) {
            finalPrice = request.getManualPrice();
            log.debug("Usando precio manual proporcionado: {} para producto {}", finalPrice, productName);
        }

        // Crear y persistir el ítem
        ShoppingItem item = ShoppingItem.builder()
                .houseId(request.getHouseId())
                .addedById(request.getAddedById())
                .name(productName)
                .estimatedPrice(finalPrice)
                .imageUrl(imageUrl)
                .status(ShoppingItemStatus.PENDING)
                .assignedUsers(request.getAssignedUserIds() != null ? request.getAssignedUserIds() : new ArrayList<>())
                .build();

        ShoppingItem savedItem = shoppingItemRepository.save(item);
        log.info("Ítem añadido exitosamente: {} (ID: {})", savedItem.getName(), savedItem.getId());

        return mapToResponse(savedItem);
    }

    /**
     * Obtiene la lista de compra completa de una vivienda.
     * Devuelve ítems PENDING y BOUGHT por separado.
     */
    @Transactional(readOnly = true)
    public ShoppingListResponse getShoppingList(UUID houseId) {
        log.debug("Obteniendo lista de compra para house: {}", houseId);

        List<ShoppingItem> pendingItems = shoppingItemRepository
                .findByHouseIdAndStatusOrderByCreatedAtDesc(houseId, ShoppingItemStatus.PENDING);

        List<ShoppingItem> boughtItems = shoppingItemRepository
                .findByHouseIdAndStatusOrderByCreatedAtDesc(houseId, ShoppingItemStatus.BOUGHT);

        Double estimatedBudget = pendingItems.stream()
                .mapToDouble(ShoppingItem::getEstimatedPrice)
                .sum();

        return ShoppingListResponse.builder()
                .houseId(houseId)
                .pendingItems(pendingItems.stream().map(this::mapToResponse).collect(Collectors.toList()))
                .boughtItems(boughtItems.stream().map(this::mapToResponse).collect(Collectors.toList()))
                .estimatedBudget(estimatedBudget)
                .build();
    }

    /**
     * Marca un ítem como comprado (cambio de estado a BOUGHT).
     */
    @Transactional
    public ShoppingItemResponse markAsBought(UUID itemId) {
        log.info("Marcando ítem como comprado: {}", itemId);

        ShoppingItem item = shoppingItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("El ítem no existe: " + itemId));

        item.setStatus(ShoppingItemStatus.BOUGHT);
        ShoppingItem updatedItem = shoppingItemRepository.save(item);

        log.info("Ítem marcado como comprado: {}", updatedItem.getId());
        return mapToResponse(updatedItem);
    }

    /**
     * Actualiza los usuarios asignados a un ítem de compra.
     */
    @Transactional
    public ShoppingItemResponse updateShoppingItemAssignees(UUID itemId, List<UUID> assigneeIds) {
        log.info("Actualizando asignaciones para el ítem de compra: {} con usuarios: {}", itemId, assigneeIds);
        ShoppingItem item = shoppingItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("El ítem no existe: " + itemId));

        item.setAssignedUsers(assigneeIds);
        ShoppingItem updatedItem = shoppingItemRepository.save(item);
        return mapToResponse(updatedItem);
    }

    /**
     * Elimina un ítem de la lista de compra.
     * Se bloquea la eliminación si el ítem ya está liquidado (historial intocable).
     */
    @Transactional
    public void deleteShoppingItem(UUID itemId) {
        log.info("Eliminando ítem de la lista de compra: {}", itemId);

        ShoppingItem item = shoppingItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("El ítem no existe: " + itemId));

        // Validación crítica: Inmutabilidad financiera
        if (ShoppingItemStatus.BOUGHT.equals(item.getStatus())) {
            throw new IllegalStateException(
                    "No se puede eliminar un producto que ya ha sido comprado y liquidado. El historial financiero es inmutable.");
        }

        shoppingItemRepository.delete(item);
        log.info("Ítem eliminado exitosamente: {}", itemId);
    }

    /**
     * Obtiene solo los ítems PENDING (sin comprar) de una vivienda.
     */
    @Transactional(readOnly = true)
    public List<ShoppingItemResponse> getPendingItems(UUID houseId) {
        log.debug("Obteniendo ítems pendientes para house: {}", houseId);

        return shoppingItemRepository
                .findByHouseIdAndStatusOrderByCreatedAtDesc(houseId, ShoppingItemStatus.PENDING)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene solo los ítems BOUGHT (comprados) de una vivienda.
     */
    @Transactional(readOnly = true)
    public List<ShoppingItemResponse> getBoughtItems(UUID houseId) {
        log.debug("Obteniendo ítems comprados para house: {}", houseId);

        return shoppingItemRepository
                .findByHouseIdAndStatusOrderByCreatedAtDesc(houseId, ShoppingItemStatus.BOUGHT)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el presupuesto estimado total de ítems PENDING.
     */
    @Transactional(readOnly = true)
    public Double getEstimatedBudget(UUID houseId) {
        log.debug("Calculando presupuesto estimado para house: {}", houseId);

        return shoppingItemRepository
                .findByHouseIdAndStatusOrderByCreatedAtDesc(houseId, ShoppingItemStatus.PENDING)
                .stream()
                .mapToDouble(ShoppingItem::getEstimatedPrice)
                .sum();
    }

    /**
     * Mapea una entidad ShoppingItem a su DTO de respuesta.
     */
    private ShoppingItemResponse mapToResponse(ShoppingItem item) {
        return ShoppingItemResponse.builder()
                .id(item.getId())
                .houseId(item.getHouseId())
                .addedById(item.getAddedById())
                .name(item.getName())
                .estimatedPrice(item.getEstimatedPrice())
                .imageUrl(item.getImageUrl())
                .status(item.getStatus())
                .createdAt(item.getCreatedAt())
                .assignedUserIds(item.getAssignedUsers())
                .checkoutId(item.getCheckoutId())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    /**
     * Procesa el checkout de la lista de compra.
     * Calcula los gastos proporcionales por participante y registra el gasto
     * exacto.
     */
    @Transactional
    public void checkoutShoppingList(CheckoutRequest request) {
        log.info("Iniciando checkout de lista de compra para houseId: {} por importe real: {}", request.getHouseId(),
                request.getTotalRealAmount());

        List<ShoppingItem> pendingItems = shoppingItemRepository
                .findByHouseIdAndStatusOrderByCreatedAtDesc(request.getHouseId(), ShoppingItemStatus.PENDING);

        if (pendingItems.isEmpty()) {
            throw new IllegalStateException("No hay productos pendientes para hacer checkout");
        }

        BigDecimal totalEstimated = BigDecimal.ZERO;
        for (ShoppingItem item : pendingItems) {
            totalEstimated = totalEstimated.add(BigDecimal.valueOf(item.getEstimatedPrice()));
        }

        if (totalEstimated.compareTo(BigDecimal.ZERO) == 0) {
            totalEstimated = BigDecimal.ONE; // fallback para evitar división por 0
        }

        List<HouseMember> allMembers = houseMemberRepository.findByHouseId(request.getHouseId());
        List<UUID> allMemberIds = allMembers.stream().map(m -> m.getUser().getId()).collect(Collectors.toList());

        Map<UUID, BigDecimal> exactSplits = new HashMap<>();
        UUID checkoutId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        for (ShoppingItem item : pendingItems) {
            BigDecimal estimated = BigDecimal.valueOf(item.getEstimatedPrice());
            BigDecimal proportion = estimated.divide(totalEstimated, 4, RoundingMode.HALF_UP);
            BigDecimal itemRealPrice = request.getTotalRealAmount().multiply(proportion);

            List<UUID> usersForItem = (item.getAssignedUsers() == null || item.getAssignedUsers().isEmpty())
                    ? allMemberIds
                    : item.getAssignedUsers();

            BigDecimal pricePerUser = itemRealPrice.divide(BigDecimal.valueOf(usersForItem.size()), 2,
                    RoundingMode.HALF_UP);

            for (UUID userId : usersForItem) {
                exactSplits.put(userId, exactSplits.getOrDefault(userId, BigDecimal.ZERO).add(pricePerUser));
            }

            item.setStatus(ShoppingItemStatus.BOUGHT);
            item.setCheckoutId(checkoutId);
            item.setUpdatedAt(now);
        }

        shoppingItemRepository.saveAll(pendingItems);

        CreateExpenseRequest expenseRequest = CreateExpenseRequest.builder()
                .title("Compra " + java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .amount(request.getTotalRealAmount())
                .houseId(request.getHouseId())
                .paidById(request.getPaidById())
                .participantIds(new ArrayList<>(exactSplits.keySet()))
                .exactSplits(exactSplits)
                .build();

        expenseService.createExpense(expenseRequest);

        log.info("Checkout completado exitosamente y gasto registrado.");
    }
}