package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.List;

@Service
public class EmbeddingService {

    // URL de l'API Hugging Face
    private final String huggingFaceApiUrl = "https://api-inference.huggingface.co/models/sentence-transformers/all-MiniLM-L6-v2";

    // Clé API Hugging Face
    private final String apiToken = "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";  // Remplace par ta clé API Hugging Face

    // Méthode pour récupérer les embeddings depuis Hugging Face
    public List<List<Float>> getEmbeddingsFromHuggingFace(String text) {
        // Création d'un RestTemplate pour envoyer des requêtes HTTP
        RestTemplate restTemplate = new RestTemplate();

        // Configuration des en-têtes HTTP avec l'API token
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Corps de la requête JSON : On envoie simplement le texte comme entrée
        String requestBody = String.format("{\"inputs\": \"%s\"}", text);

        // Création de l'entité HTTP avec le corps et les en-têtes
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // Envoi de la requête POST à Hugging Face et récupération de la réponse
        try {
            // On suppose que la réponse sera une liste d'embeddings sous la forme d'une liste de liste de flottants
            ResponseEntity<List> response = restTemplate.exchange(
                    huggingFaceApiUrl,
                    HttpMethod.POST,
                    entity,
                    List.class
            );

            // Vérifier la réponse
            if (response.getStatusCode() == HttpStatus.OK) {
                // Ici on suppose que la réponse est une liste de listes de flottants (embeddings)
                return response.getBody();  // La réponse devrait être une liste de [List<Float>]
            } else {
                throw new RuntimeException("Erreur lors de la récupération des embeddings. Status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            // Gestion des erreurs
            throw new RuntimeException("Erreur de communication avec l'API Hugging Face", e);
        }
    }
}
