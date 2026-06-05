package com.kpiso.api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void handleIllegalArgumentExceptionShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/dummy-illegal"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Illegal argument test"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void handleGenericExceptionShouldReturnInternalServerError() throws Exception {
        mockMvc.perform(get("/dummy-generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.error").value("Generic exception test"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void handleValidationExceptionShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/dummy-valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").value("must not be blank"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @RestController
    static class DummyController {

        @GetMapping("/dummy-illegal")
        public void triggerIllegal() {
            throw new IllegalArgumentException("Illegal argument test");
        }

        @GetMapping("/dummy-generic")
        public void triggerGeneric() throws Exception {
            throw new Exception("Generic exception test");
        }

        @PostMapping("/dummy-valid")
        public void triggerValidation(@Valid @RequestBody DummyDto dto) {
        }
    }

    @Getter
    @Setter
    static class DummyDto {
        @NotBlank(message = "must not be blank")
        private String name;
    }
}
