package com.kpiso.api.modules.shoppinglist.infrastructure.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kpiso.api.modules.shoppinglist.infrastructure.client.ProductDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ScraperServiceAdapterTest {

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private OpenFoodFactsAdapter fallbackAdapter;
    private ScraperServiceAdapter adapter;

    @BeforeEach
    void setUp() {
        restTemplate = Mockito.mock(RestTemplate.class);
        objectMapper = new ObjectMapper();
        fallbackAdapter = Mockito.mock(OpenFoodFactsAdapter.class);
        adapter = new ScraperServiceAdapter(restTemplate, objectMapper, fallbackAdapter, "http://test-scraper");
    }

    @Test
    void searchProductShouldReturnResultWhenScraperSucceeds() {
        String json = "[{\"name\":\"Leche\",\"imageUrl\":\"img.jpg\",\"mainCategory\":\"dairy\",\"categoryTags\":\"dairy,milk\"}]";
        when(restTemplate.getForObject(any(String.class), eq(String.class))).thenReturn(json);

        ProductDetails details = adapter.searchProduct("leche");

        assertNotNull(details);
        assertTrue(details.isFound());
        assertEquals("Leche", details.getName());
        assertEquals("img.jpg", details.getImageUrl());
        assertEquals("dairy", details.getMainCategory());
        assertEquals("dairy,milk", details.getCategoryTags());
    }

    @Test
    void searchProductShouldReturnNotFoundWhenNoResultsAndFallbackDisabled() {
        when(restTemplate.getForObject(any(String.class), eq(String.class))).thenReturn("[]");

        ProductDetails details = adapter.searchProduct("leche");

        assertNotNull(details);
        assertFalse(details.isFound());
    }

    @Test
    void searchProductSuggestionsShouldUseFallbackWhenScraperUnavailableAndFallbackEnabled() {
        when(restTemplate.getForObject(any(String.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        ProductDetails mockFallbackDetail = ProductDetails.builder().name("Fallback Milk").found(true).build();
        when(fallbackAdapter.searchProductSuggestions(eq("leche")))
                .thenReturn(List.of(mockFallbackDetail));

        List<ProductDetails> results = adapter.searchProductSuggestions("leche", true);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Fallback Milk", results.get(0).getName());
        verify(fallbackAdapter, times(1)).searchProductSuggestions("leche");
    }

    @Test
    void searchProductSuggestionsShouldReturnEmptyListWhenScraperResponseNotArray() {
        when(restTemplate.getForObject(any(String.class), eq(String.class))).thenReturn("{\"error\":\"not found\"}");

        List<ProductDetails> results = adapter.searchProductSuggestions("leche", false);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void searchProductSuggestionsShouldSkipItemWithEmptyName() {
        String json = "[{\"name\":\"\",\"imageUrl\":\"img.png\"}, {\"name\":\"Milk\",\"imageUrl\":\"img.png\"}]";
        when(restTemplate.getForObject(any(String.class), eq(String.class))).thenReturn(json);

        List<ProductDetails> results = adapter.searchProductSuggestions("leche", false);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Milk", results.get(0).getName());
    }

    @Test
    void searchProductSuggestionsShouldReturnEmptyListWhenResponseIsNull() {
        when(restTemplate.getForObject(any(String.class), eq(String.class))).thenReturn(null);

        List<ProductDetails> results = adapter.searchProductSuggestions("leche", false);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
