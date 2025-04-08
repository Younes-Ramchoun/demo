package com.example.demo.config; // Assure-toi que le package correspond à ton projet

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**") // Appliquer CORS sur toutes les routes API
                        .allowedOrigins("*") // Autoriser toutes les origines
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Autoriser toutes les méthodes HTTP
                        .allowedHeaders("*") // Autoriser tous les headers
                        .allowCredentials(false); // Ne pas exiger d'authentification
            }
        };
    }
}
