package com.kpiso.api.modules.shoppinglist.infrastructure.adapter;

import com.kpiso.api.modules.shoppinglist.infrastructure.client.ProductCatalogClient;
import com.kpiso.api.modules.shoppinglist.infrastructure.client.ProductDetails;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter que consulta el microservicio kpiso-scraper en lugar de llamar
 * directamente a OpenFoodFacts.
 *
 * El microservicio sirve datos desde un JSON local pre-cargado, eliminando
 * la latencia de las llamadas externas en tiempo real.
 *
 * Si el scraper no está disponible, delega al OpenFoodFactsAdapter como fallback.
 * Marcado como @Primary para que Spring lo inyecte donde se espera ProductCatalogClient.
 */
@Slf4j
@Primary
@Component
public class ScraperServiceAdapter implements ProductCatalogClient {

    private static final int MAX_SUGGESTIONS = 5;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OpenFoodFactsAdapter fallbackAdapter;
    private final String scraperBaseUrl;

    public ScraperServiceAdapter(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            OpenFoodFactsAdapter fallbackAdapter,
            @Value("${scraper.service.url:http://kpiso-scraper:8081}") String scraperBaseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.fallbackAdapter = fallbackAdapter;
        this.scraperBaseUrl = scraperBaseUrl;
    }

    /**
     * Busca un producto priorizando el microservicio scraper.
     * Fallback a OpenFoodFacts si el scraper no está disponible o no encuentra resultados.
     */
    @Override
    public ProductDetails searchProduct(String query) {
        List<ProductDetails> suggestions = searchProductSuggestions(query);
        return suggestions.isEmpty()
                ? ProductDetails.createNotFound(query)
                : suggestions.get(0);
    }

    /**
     * Busca múltiples sugerencias de productos consultando kpiso-scraper.
     * Si el servicio no responde o devuelve vacío, usa OpenFoodFactsAdapter como fallback.
     */
    public List<ProductDetails> searchProductSuggestions(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            String url = scraperBaseUrl + "/products?q=" + encodedQuery;

            log.debug("Consultando kpiso-scraper para: '{}'", query);
            String response = restTemplate.getForObject(url, String.class);

            List<ProductDetails> results = parseScraperResponse(response);

            if (!results.isEmpty()) {
                log.debug("Scraper hit: {} resultados para '{}'", results.size(), query);
                return results;
            }

            log.debug("Scraper devolvió vacío para '{}', usando fallback OpenFoodFacts", query);

        } catch (RestClientException e) {
            log.warn("kpiso-scraper no disponible para '{}': {}. Usando fallback.", query, e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado consultando kpiso-scraper para '{}': {}", query, e.getMessage());
        }

        // Fallback: delegar a OpenFoodFactsAdapter
        return fallbackAdapter.searchProductSuggestions(query);
    }

    /**
     * Parsea la respuesta JSON del microservicio scraper.
     * Formato esperado: array de objetos con name, imageUrl, mainCategory, categoryTags.
     */
    private List<ProductDetails> parseScraperResponse(String jsonResponse) {
        List<ProductDetails> results = new ArrayList<>();
        if (jsonResponse == null || jsonResponse.isBlank()) {
            return results;
        }

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);

            if (!root.isArray()) {
                log.warn("Respuesta del scraper inesperada: no es un array JSON");
                return results;
            }

            for (int i = 0; i < Math.min(root.size(), MAX_SUGGESTIONS); i++) {
                JsonNode node = root.get(i);
                String name = node.path("name").asText(null);

                if (name == null || name.isBlank()) {
                    continue;
                }

                results.add(ProductDetails.builder()
                        .name(name.trim())
                        .imageUrl(node.path("imageUrl").asText(null))
                        .mainCategory(node.path("mainCategory").asText(null))
                        .categoryTags(node.path("categoryTags").asText(""))
                        .found(true)
                        .build());
            }

        } catch (Exception e) {
            log.error("Error parseando respuesta del scraper: {}", e.getMessage());
        }

        return results;
    }
}
