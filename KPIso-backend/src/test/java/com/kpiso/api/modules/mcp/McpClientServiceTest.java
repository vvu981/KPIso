package com.kpiso.api.modules.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class McpClientServiceTest {

    private McpClientService mcpClientService;
    private RestTemplate mockRestTemplate;

    @BeforeEach
    void setUp() {
        mcpClientService = new McpClientService("http://test-mcp");
        mockRestTemplate = Mockito.mock(RestTemplate.class);
        ReflectionTestUtils.setField(mcpClientService, "restTemplate", mockRestTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void queryMcpChatShouldReturnResponseOnSuccess() {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("intent", "ADD_PRODUCT");
        mockResponse.put("message", "Success");
        mockResponse.put("products", Collections.emptyList());

        when(mockRestTemplate.postForObject(eq("http://test-mcp/chat"), any(Map.class), eq(Map.class)))
                .thenReturn(mockResponse);

        Map<String, Object> result = mcpClientService.queryMcpChat("Hola");

        assertNotNull(result);
        assertEquals("ADD_PRODUCT", result.get("intent"));
        assertEquals("Success", result.get("message"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void queryMcpChatShouldReturnErrorMapOnNullResponse() {
        when(mockRestTemplate.postForObject(eq("http://test-mcp/chat"), any(Map.class), eq(Map.class)))
                .thenReturn(null);

        Map<String, Object> result = mcpClientService.queryMcpChat("Hola");

        assertNotNull(result);
        assertEquals("OTHER", result.get("intent"));
        assertTrue(result.get("message").toString().contains("Error al procesar la respuesta"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void queryMcpChatShouldReturnFallbackMapOnRestClientException() {
        when(mockRestTemplate.postForObject(eq("http://test-mcp/chat"), any(Map.class), eq(Map.class)))
                .thenThrow(new RestClientException("Connection timeout"));

        Map<String, Object> result = mcpClientService.queryMcpChat("Hola");

        assertNotNull(result);
        assertEquals("OTHER", result.get("intent"));
        assertTrue(result.get("message").toString().contains("no está disponible actualmente"));
        assertTrue(result.get("message").toString().contains("Connection timeout"));
    }
}
