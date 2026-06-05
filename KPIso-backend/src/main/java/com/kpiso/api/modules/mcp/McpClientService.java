package com.kpiso.api.modules.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

/**
 * Servicio cliente que interactúa con el microservicio kpiso-mcp.
 */
@Slf4j
@Service
public class McpClientService {

    private final RestTemplate restTemplate;
    private final String mcpServiceUrl;

    public McpClientService(
            @Value("${mcp.service.url:http://kpiso-mcp:3000}") String mcpServiceUrl) {
        this.mcpServiceUrl = mcpServiceUrl;
        
        // Crear un RestTemplate dedicado con 30 segundos de read timeout para las consultas de la IA
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Envía una consulta de chat al servidor de IA/MCP de Node.js.
     * Devuelve la respuesta estructurada que contiene la intención, el mensaje de la IA
     * y los productos sugeridos mapeados desde products.json.
     */
    public Map<String, Object> queryMcpChat(String message) {
        String url = mcpServiceUrl + "/chat";
        log.info("Enviando petición a la IA en kpiso-mcp: '{}'", message);

        Map<String, String> request = new HashMap<>();
        request.put("message", message);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            if (response != null) {
                log.info("Respuesta recibida exitosamente de kpiso-mcp");
                return response;
            }
        } catch (RestClientException e) {
            log.error("Error al conectar con el servicio mcp en {}: {}", url, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("intent", "OTHER");
            errorResponse.put("message", "Lo siento, el asistente de IA no está disponible actualmente. Detalle: " + e.getMessage());
            errorResponse.put("products", java.util.Collections.emptyList());
            return errorResponse;
        }

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("intent", "OTHER");
        errorResponse.put("message", "Error al procesar la respuesta del asistente de IA.");
        errorResponse.put("products", java.util.Collections.emptyList());
        return errorResponse;
    }
}
