package com.example.demo.service;

import com.example.demo.dto.*; // Importer les DTOs
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    // --- INJECTIONS MODIFIÉES ---
    private final RetrievalService retrievalService; // Gardé
    private final RestTemplate restTemplate;         // Gardé (pour appeler Ollama)
    private final ObjectMapper objectMapper;         // Gardé (pour préparer/parser JSON Ollama)
    // private final DeepInfraService deepInfraService; // <<< SUPPRIMÉ (ou commenté si vous voulez le garder pour autre chose)

    // --- CONFIGURATION OLLAMA (ajoutée si pas déjà là) ---
    @Value("${ollama.base.url}")
    private String ollamaBaseUrl;
    @Value("${ollama.model.chat}") // Nom du modèle de chat (ex: llama3)
    private String ollamaChatModel;


    // Constantes pour le retrieval (gardées)
    private static final int DEFAULT_TOP_K = 3;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.7;

    // --- CONSTRUCTEUR MODIFIÉ ---
    @Autowired
    public RagService(RetrievalService retrievalService,
                      RestTemplate restTemplate, // <<< GARDÉ
                      ObjectMapper objectMapper) {  // <<< GARDÉ
        // DeepInfraService deepInfraService <<< SUPPRIMÉ
        this.retrievalService = retrievalService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        // this.deepInfraService = deepInfraService; // <<< SUPPRIMÉ
    }

    // --- MÉTHODE QUERY MODIFIÉE ---
    public RagQueryResponse query(RagQueryRequest request) {
        String question = request.getQuestion();
        log.info("Traitement de la requête RAG locale pour la question : '{}'", question);

        int topK = DEFAULT_TOP_K;
        double threshold = DEFAULT_SIMILARITY_THRESHOLD;

        // 1. Appel au RetrievalService existant (INCHANGÉ)
        log.debug("Appel du RetrievalService (topK={}, threshold={})", topK, threshold);
        SearchRequest searchRequest = new SearchRequest(question, topK, threshold);
        SearchResult searchResult = retrievalService.searchSimilarChunks(searchRequest);
        List<RetrievedChunk> relevantChunks = searchResult.getRelevantChunks();
        log.info("{} chunks pertinents trouvés par RetrievalService.", searchResult.getResultsFound());

        // 2. Construction du Prompt pour Ollama Chat (INCHANGÉ)
        String context = relevantChunks.stream()
                .map(RetrievedChunk::getChunkText)
                .collect(Collectors.joining("\n\n---\n\n"));

        String prompt;
        if (context.isBlank()) {
            log.warn("Aucun contexte pertinent trouvé. Le prompt contiendra uniquement la question.");
            prompt = String.format("Réponds directement à la question suivante : %s", question);
        } else {
            prompt = String.format(
                    "En te basant STRICTEMENT sur le contexte suivant:\n\n" +
                            "--- CONTEXTE ---\n" +
                            "%s\n" +
                            "--- FIN CONTEXTE ---\n\n" +
                            "Réponds à la question suivante : %s",
                    context,
                    question
            );
        }
        log.debug("Prompt construit pour Ollama (modèle {}):\n{}", ollamaChatModel, prompt);

        // 3. Appel à l'API Chat d'Ollama <<< MODIFICATION ICI : Appel local Ollama
        log.debug("Appel à l'API Chat d'Ollama...");
        String generatedAnswer = callOllamaChatApi(prompt); // <<< APPELER LA MÉTHODE OLLAMA

        /* <<< SUPPRIMER ou COMMENTER l'ancien appel à DeepInfra >>>
        log.debug("Appel du DeepInfraService...");
        Map<String, Object> deepInfraResponseMap;
        try {
            deepInfraResponseMap = deepInfraService.sendPrompt(prompt);
        } catch (Exception e) {
            log.error("Erreur lors de l'appel à DeepInfraService: {}", e.getMessage(), e);
            return new RagQueryResponse("Désolé, une erreur s'est produite lors de la génération de la réponse.", Collections.emptyList());
        }

        // 4. Extraction de la réponse de DeepInfra <<< SUPPRIMER ou COMMENTER
        String answer = (String) deepInfraResponseMap.getOrDefault("generated_text", ... ); // Adapter clé
        log.info("Réponse obtenue de DeepInfra.");
        */

        // 4. (Nouvelle numérotation) Construction de la réponse finale RAG
        if (generatedAnswer == null || generatedAnswer.isBlank()) {
            log.error("Aucune réponse générée par Ollama ou erreur lors de l'appel.");
            generatedAnswer = "Désolé, je n'ai pas pu générer de réponse pour le moment.";
        }

        return new RagQueryResponse(generatedAnswer.trim(), relevantChunks);
    }


    // >>> AJOUT/VÉRIFICATION de cette méthode pour appeler Ollama <<<
    /**
     * Appelle l'API /api/chat d'Ollama.
     * @param userPrompt Le prompt complet incluant contexte et question.
     * @return La réponse textuelle de l'assistant, ou null en cas d'erreur.
     */
    private String callOllamaChatApi(String userPrompt) {
        String ollamaApiUrl = ollamaBaseUrl + "/api/chat";
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ollamaChatModel); // Utilise le modèle configuré (ex: llama3)
        requestBody.put("stream", false);
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", userPrompt)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    ollamaApiUrl, HttpMethod.POST, requestEntity, Map.class
            );

            // Extraction de la réponse Ollama Chat API (message.content)
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody.containsKey("message")) {
                    try {
                        Map<String, Object> message = (Map<String, Object>) responseBody.get("message");
                        if ("assistant".equals(message.get("role")) && message.containsKey("content")) {
                            String answer = (String) message.get("content");
                            log.info("Réponse reçue d'Ollama Chat API.");
                            return answer;
                        }
                    } catch (ClassCastException e) { /* Log error */ }
                }
            }
            log.error("Erreur/Réponse invalide d'Ollama Chat API: Status={}, Body={}", response.getStatusCode(), response.getBody());
        } catch (RestClientException e) {
            log.error("Erreur RestClient lors de l'appel à Ollama Chat API {}: {}", ollamaApiUrl, e.getMessage());
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'appel à Ollama Chat API: {}", e.getMessage(), e);
        }
        return null; // Échec
    }
}