package com.example.demo.service;

import com.example.demo.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeepInfraRagService { // <<< NOUVEAU NOM

    private static final Logger log = LoggerFactory.getLogger(DeepInfraRagService.class);

    // Injecter les services nécessaires
    private final RetrievalService retrievalService;
    private final DeepInfraService deepInfraService; // <<< Service pour DeepInfra

    // Constantes retrieval
    private static final int DEFAULT_TOP_K = 3;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.7;

    @Autowired
    public DeepInfraRagService(RetrievalService retrievalService,
                               DeepInfraService deepInfraService) {
        this.retrievalService = retrievalService;
        this.deepInfraService = deepInfraService;
    }

    public RagQueryResponse queryWithDeepInfra(RagQueryRequest request) { // <<< Nom méthode changé
        String question = request.getQuestion();
        log.info("Traitement de la requête RAG (via DeepInfra) pour la question : '{}'", question);

        int topK = DEFAULT_TOP_K;
        double threshold = DEFAULT_SIMILARITY_THRESHOLD;

        // 1. Appel au RetrievalService (local Ollama embedding + pgvector)
        log.debug("Appel du RetrievalService (topK={}, threshold={})", topK, threshold);
        SearchRequest searchRequest = new SearchRequest(question, topK, threshold);
        SearchResult searchResult = retrievalService.searchSimilarChunks(searchRequest);
        List<RetrievedChunk> relevantChunks = searchResult.getRelevantChunks();
        log.info("{} chunks pertinents trouvés par RetrievalService.", searchResult.getResultsFound());

        // 2. Construction du Prompt pour DeepInfra
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
        log.debug("Prompt construit pour DeepInfra:\n{}", prompt.substring(0, Math.min(prompt.length(), 200))+"...");

        // 3. Appel au DeepInfraService
        log.debug("Appel du DeepInfraService...");
        Map<String, Object> deepInfraResponseMap;
        String generatedAnswer = "Erreur: Impossible d'extraire la réponse de DeepInfra."; // Défaut

        try {
            // Utiliser la méthode existante de DeepInfraService
            deepInfraResponseMap = deepInfraService.sendPrompt(prompt);

            // Extraire la réponse depuis la Map (adapter la clé si nécessaire)
            if (deepInfraResponseMap != null && deepInfraResponseMap.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) deepInfraResponseMap.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    if (firstChoice.containsKey("message")) {
                        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                        if (message.containsKey("content")) {
                            generatedAnswer = (String) message.get("content");
                            log.info("Réponse extraite avec succès de DeepInfra.");
                        } else { log.error("Champ 'content' manquant dans 'message'"); }
                    } else { log.error("Champ 'message' manquant dans 'choice[0]'"); }
                } else { log.error("Liste 'choices' vide"); }
            } else { log.error("Réponse DeepInfra nulle ou sans clé 'choices'."); }

        } catch (Exception e) {
            log.error("Erreur lors de l'appel à DeepInfraService: {}", e.getMessage(), e);
            generatedAnswer = "Désolé, une erreur s'est produite lors de la génération de la réponse.";
        }

        // 4. Construction de la réponse finale RAG
        return new RagQueryResponse(generatedAnswer.trim(), relevantChunks);
    }
}