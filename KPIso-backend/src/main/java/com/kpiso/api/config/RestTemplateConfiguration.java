package com.kpiso.api.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuración de RestTemplate para llamadas HTTP al catálogo de productos.
 * 
 * Define timeouts y configuración de reintentos para garantizar robustez
 * ante fallos de conectividad.
 */
@Configuration
public class RestTemplateConfiguration {

    /**
     * Bean de RestTemplate configurado con timeouts apropiados.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}
