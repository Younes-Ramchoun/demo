package com.example.demo.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class DeepInfraService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String API_URL = "https://api.deepinfra.com/v1/openai/chat/completions";
    private static final String API_KEY = "mTU9GXGzJC84Vqy3lhQuOJus3PD1Fy0d";  // Remplacer avec un token valide

    public Map<String, Object> sendPrompt(String userMessage) {
        // 1. Définir les en-têtes HTTP
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. Construire le corps de la requête avec LLaMA
        Map<String, Object> requestBody = Map.of(
                "model", "meta-llama/Meta-Llama-3.1-8B-Instruct",  // Modèle LLaMA correct
                "messages", new Object[]{
                        Map.of("role", "user", "content", userMessage)
                }
        );

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        // 3. Envoyer la requête POST
        ResponseEntity<Map> response = restTemplate.exchange(
                API_URL, HttpMethod.POST, requestEntity, Map.class
        );

        // Retourner la réponse du modèle sous forme de Map
        return response.getBody();
    }
}
