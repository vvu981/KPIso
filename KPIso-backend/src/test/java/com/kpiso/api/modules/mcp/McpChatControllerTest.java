package com.kpiso.api.modules.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(McpChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class McpChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private McpClientService mcpClientService;

    @Test
    void chatShouldReturnOkWhenMessageIsValid() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("message", "Hola");

        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("intent", "ADD_PRODUCT");
        mockResponse.put("message", "Añadido");

        when(mcpClientService.queryMcpChat(eq("Hola"))).thenReturn(mockResponse);

        mockMvc.perform(post("/shopping-list/mcp/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void chatShouldReturnBadRequestWhenMessageIsNull() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("message", null);

        mockMvc.perform(post("/shopping-list/mcp/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatShouldReturnBadRequestWhenMessageIsEmpty() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("message", "   ");

        mockMvc.perform(post("/shopping-list/mcp/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
