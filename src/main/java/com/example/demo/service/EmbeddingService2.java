package com.example.demo.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class EmbeddingService2 {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String OLLAMA_API_URL = "http://localhost:11434/api/embeddings"; // Ollama tourne en local

    public List<float[]> generateEmbeddings(List<String> chunks) {
        List<float[]> embeddings = new ArrayList<>();

        for (String chunk : chunks) {
            try {
                // Création du corps de la requête
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", "nomic-embed-text");  // Utilisation du modèle d'embedding
                requestBody.put("prompt", chunk);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

                // Envoi de la requête POST vers Ollama
                ResponseEntity<Map> response = restTemplate.exchange(
                        OLLAMA_API_URL,
                        HttpMethod.POST,
                        requestEntity,
                        Map.class
                );

                // Extraction des embeddings depuis la réponse
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    List<Double> embeddingList = (List<Double>) response.getBody().get("embedding");
                    float[] embeddingArray = new float[embeddingList.size()];

                    for (int i = 0; i < embeddingList.size(); i++) {
                        embeddingArray[i] = embeddingList.get(i).floatValue();
                    }
                    embeddings.add(embeddingArray);
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de la génération des embeddings pour le chunk : " + chunk);
                e.printStackTrace();
            }
        }
        return embeddings;
    }
}
