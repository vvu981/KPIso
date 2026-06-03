package com.kpiso.api.modules.shoppinglist.infrastructure.adapter;

import com.kpiso.api.modules.shoppinglist.infrastructure.client.ProductCatalogClient;
import com.kpiso.api.modules.shoppinglist.infrastructure.client.ProductDetails;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador para consultar el catálogo Open Food Facts.
 * Implementa la interfaz ProductCatalogClient.
 * 
 * Proporciona manejo robusto de errores y parsing del JSON de respuesta.
 */
@Slf4j
@Component
public class OpenFoodFactsAdapter implements ProductCatalogClient {

    private static final String OPEN_FOOD_FACTS_URL = "https://world.openfoodfacts.org/cgi/search.pl";
    private static final String DEFAULT_IMAGE_URL = "https://via.placeholder.com/150?text=No+Image";
    private static final int MAX_SUGGESTIONS = 5;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openfoody.facts.timeout:5000}")
    private long timeout;

    public OpenFoodFactsAdapter(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ProductDetails searchProduct(String query) {
        List<ProductDetails> suggestions = searchProductSuggestions(query);
        return suggestions.isEmpty() ? ProductDetails.createNotFound(query) : suggestions.get(0);
    }

    /**
     * Busca múltiples sugerencias de productos en Open Food Facts
     */
    public List<ProductDetails> searchProductSuggestions(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = String.format(
                    "%s?search_terms=%s&search_simple=1&action=process&json=1&page_size=20",
                    OPEN_FOOD_FACTS_URL,
                    encodedQuery
            );

            log.debug("Consultando Open Food Facts para sugerencias: {}", query);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "KPIso/1.0 - AcademicApp/JavaSpring - https://github.com/kpiso/KPIso");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            String body = responseEntity.getBody();
            if (body != null && (body.contains("temporarily unavailable") || body.contains("experiencing unusually high demand") || body.contains("limit"))) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "La API externa de Open Food Facts está sobrecargada o bloqueando peticiones debido a alta demanda.");
            }

            return parseOpenFoodFactsResponse(body);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("Error al conectar con Open Food Facts para la búsqueda: {}", query, e);
            if (e.getMessage() != null && (e.getMessage().contains("429") || e.getMessage().contains("503") || e.getMessage().contains("Limit"))) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "La API externa de Open Food Facts está sobrecargada o limitando las peticiones.");
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error inesperado al procesar búsqueda en Open Food Facts: {}", query, e);
            return new ArrayList<>();
        }
    }

    /**
     * Parsea la respuesta JSON de Open Food Facts y extrae hasta MAX_SUGGESTIONS productos válidos
     */
    private List<ProductDetails> parseOpenFoodFactsResponse(String jsonResponse) {
        List<ProductDetails> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode products = root.get("products");

            if (products == null || products.isEmpty()) {
                log.debug("No se encontraron productos en la respuesta de Open Food Facts");
                return results;
            }

            for (int i = 0; i < Math.min(products.size(), MAX_SUGGESTIONS); i++) {
                JsonNode product = products.get(i);
                ProductDetails details = parseProduct(product);
                if (details != null) {
                    results.add(details);
                }
            }

        } catch (IOException e) {
            log.error("Error al parsear respuesta JSON de Open Food Facts", e);
        } catch (Exception e) {
            log.error("Error inesperado al procesar respuesta de Open Food Facts", e);
        }

        return results;
    }

    /**
     * Parsea un producto individual del JSON
     */
    private ProductDetails parseProduct(JsonNode product) {
        String productName = extractString(product, "product_name", "name_translations");
        
        if (productName == null || productName.isBlank()) {
            return null;
        }

        String imageUrl = extractImageUrl(product);
        String categoryTags = extractCategoryTags(product);

        return ProductDetails.builder()
                .name(productName)
                .imageUrl(imageUrl)
                .mainCategory(extractMainCategory(product))
                .categoryTags(categoryTags)
                .found(true)
                .build();
    }

    /**
     * Extrae un valor string del JSON, intentando múltiples rutas
     */
    private String extractString(JsonNode node, String... paths) {
        for (String path : paths) {
            JsonNode value = node.get(path);
            if (value != null && !value.isNull()) {
                String strValue = value.asText();
                if (!strValue.isBlank()) {
                    return strValue;
                }
            }
        }
        return null;
    }

    /**
     * Extrae y valida la URL de imagen del producto
     */
    private String extractImageUrl(JsonNode product) {
        String imageUrl = extractString(product, "image_url", "image_front_url", "image_small_url");
        
        if (imageUrl == null || imageUrl.isBlank()) {
            return DEFAULT_IMAGE_URL;
        }

        // Asegurar que sea una URL absoluta
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }

        // Si es relativa, convertir a absoluta
        if (imageUrl.startsWith("/")) {
            return "https://world.openfoodfacts.org" + imageUrl;
        }

        return DEFAULT_IMAGE_URL;
    }

    /**
     * Extrae la categoría principal del producto
     */
    private String extractMainCategory(JsonNode product) {
        JsonNode categories = product.get("categories_tags");
        if (categories != null && categories.isArray() && categories.size() > 0) {
            return categories.get(0).asText();
        }
        return null;
    }

    /**
     * Extrae todos los tags de categoría como string delimitado por comas
     */
    private String extractCategoryTags(JsonNode product) {
        JsonNode categories = product.get("categories_tags");
        if (categories != null && categories.isArray()) {
            StringBuilder tags = new StringBuilder();
            for (int i = 0; i < categories.size(); i++) {
                if (i > 0) tags.append(",");
                tags.append(categories.get(i).asText());
            }
            return tags.toString();
        }
        return "";
    }
}

