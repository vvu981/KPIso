package com.kpiso.api.modules.shoppinglist.infrastructure.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kpiso.api.modules.shoppinglist.infrastructure.client.ProductDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class OpenFoodFactsAdapterTest {

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private OpenFoodFactsAdapter adapter;

    @BeforeEach
    void setUp() {
        restTemplate = Mockito.mock(RestTemplate.class);
        objectMapper = new ObjectMapper();
        adapter = new OpenFoodFactsAdapter(restTemplate, objectMapper);
    }

    @Test
    void searchProductShouldReturnNotFoundWhenNoResults() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"products\":[]}", HttpStatus.OK));

        ProductDetails details = adapter.searchProduct("leche");

        assertNotNull(details);
        assertFalse(details.isFound());
        assertEquals("leche", details.getName());
    }

    @Test
    void searchProductSuggestionsShouldReturnListOnValidJson() {
        String json = "{\"products\":[" +
                "{\"product_name\":\"Leche Entera\",\"image_url\":\"http://test.com/img.jpg\",\"categories_tags\":[\"en:milks\",\"en:dairies\"]}," +
                "{\"product_name\":\"\",\"image_url\":\"http://test.com/img.jpg\"}," +
                "{\"product_name\":\"Leche Desnatada\",\"image_url\":\"/img/relative.jpg\"}" +
                "]}";

        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        List<ProductDetails> suggestions = adapter.searchProductSuggestions("leche");

        assertNotNull(suggestions);
        assertEquals(2, suggestions.size());
        assertEquals("Leche Entera", suggestions.get(0).getName());
        assertEquals("http://test.com/img.jpg", suggestions.get(0).getImageUrl());
        assertEquals("en:milks", suggestions.get(0).getMainCategory());
        assertEquals("en:milks,en:dairies", suggestions.get(0).getCategoryTags());

        assertEquals("Leche Desnatada", suggestions.get(1).getName());
        assertEquals("https://world.openfoodfacts.org/img/relative.jpg", suggestions.get(1).getImageUrl());
    }

    @Test
    void searchProductSuggestionsShouldUsePlaceholderImageIfMissing() {
        String json = "{\"products\":[{\"product_name\":\"Leche Sin Lactosa\"}]}";

        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        List<ProductDetails> suggestions = adapter.searchProductSuggestions("leche");

        assertNotNull(suggestions);
        assertEquals(1, suggestions.size());
        assertEquals("https://via.placeholder.com/150?text=No+Image", suggestions.get(0).getImageUrl());
    }

    @Test
    void searchProductSuggestionsShouldThrowTooManyRequestsWhenBodyContainsUnavailable() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("The service is temporarily unavailable due to high demand", HttpStatus.OK));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            adapter.searchProductSuggestions("leche");
        });

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
    }

    @Test
    void searchProductSuggestionsShouldThrowTooManyRequestsWhenRestClientExceptionHas429() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Error 429: Too Many Requests"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            adapter.searchProductSuggestions("leche");
        });

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
    }

    @Test
    void searchProductSuggestionsShouldReturnEmptyListWhenGenericRestClientException() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection Timeout"));

        List<ProductDetails> suggestions = adapter.searchProductSuggestions("leche");

        assertNotNull(suggestions);
        assertTrue(suggestions.isEmpty());
    }

    @Test
    void searchProductSuggestionsShouldReturnEmptyListWhenParsingExceptionOccurs() {
        // Invalid JSON to trigger parser error
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{invalid-json", HttpStatus.OK));

        List<ProductDetails> suggestions = adapter.searchProductSuggestions("leche");

        assertNotNull(suggestions);
        assertTrue(suggestions.isEmpty());
    }
}
