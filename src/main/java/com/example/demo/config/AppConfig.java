package com.example.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder; // Importer pour configurer
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory; // Importer
import org.springframework.http.client.SimpleClientHttpRequestFactory; // Importer
import org.springframework.web.client.RestTemplate;

import java.time.Duration; // Importer Duration

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) { // Injecter RestTemplateBuilder

        // Configurer des timeouts plus longs (ex: 2 minutes)
        Duration connectTimeout = Duration.ofSeconds(10); // Temps pour établir la connexion
        Duration readTimeout = Duration.ofMinutes(2);    // Temps MAXIMAL pour recevoir la réponse après connexion

        // Méthode 1: Via RestTemplateBuilder (plus moderne)
        return builder
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .build();

        /*
        // Méthode 2: Via SimpleClientHttpRequestFactory (plus ancienne mais fonctionne)
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) connectTimeout.toMillis());
        factory.setReadTimeout((int) readTimeout.toMillis());
        return new RestTemplate(factory);
        */
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}