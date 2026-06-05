package com.kpiso.api.modules.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para interactuar con el chat del asistente de IA (MCP).
 */
@Slf4j
@RestController
@RequestMapping("/shopping-list/mcp")
public class McpChatController {

    private final McpClientService mcpClientService;

    public McpChatController(McpClientService mcpClientService) {
        this.mcpClientService = mcpClientService;
    }

    /**
     * Endpoint para enviar un mensaje al asistente de IA y recibir sugerencias.
     * Espera un JSON con el campo "message".
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        log.info("POST /shopping-list/mcp/chat - Mensaje recibido: '{}'", message);

        if (message == null || message.trim().isEmpty()) {
            log.warn("El mensaje recibido es nulo o vacío.");
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> result = mcpClientService.queryMcpChat(message.trim());
        return ResponseEntity.ok(result);
    }
}
