package com.example.demo.service;

import com.example.demo.dto.RetrievedChunk;
import com.example.demo.dto.SearchRequest;
import com.example.demo.dto.SearchResult;
import com.example.demo.repository.ChunkEmbeddingRepository;
import com.example.demo.repository.projection.RetrievedChunkProjection;
import com.example.demo.util.VectorUtils; // Importer l'utilitaire

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils; // Pour vérifier les listes vides

import java.util.List;
import java.util.ArrayList;
import java.util.Collections; // Pour List.of()

@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    private final EmbeddingService2 embeddingService; // Votre service existant pour l'embedding Ollama
    private final ChunkEmbeddingRepository repository;

    @Autowired
    public RetrievalService(EmbeddingService2 embeddingService, ChunkEmbeddingRepository repository) {
        this.embeddingService = embeddingService;
        this.repository = repository;
    }

    public SearchResult searchSimilarChunks(SearchRequest request) {
        log.info("Recherche lancée - Question: '{}', topK={}, similarityThreshold={}",
                request.getQuestion(), request.getTopK(), request.getSimilarityThreshold());

        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            log.warn("Question vide fournie pour la recherche.");
            return new SearchResult(0, Collections.emptyList());
        }

        // 1. Générer l'embedding pour la question
        List<float[]> questionEmbeddings = embeddingService.generateEmbeddings(List.of(request.getQuestion()));
        if (CollectionUtils.isEmpty(questionEmbeddings)) {
            log.error("Impossible de générer l'embedding pour la question : '{}'", request.getQuestion());
            // Peut-être lancer une exception ici ? Pour l'instant, retour vide.
            return new SearchResult(0, Collections.emptyList());
        }
        float[] questionEmbedding = questionEmbeddings.get(0);
        String questionEmbeddingString = VectorUtils.floatArrayToString(questionEmbedding); // Convertir en string '[...]'
        log.debug("Embedding de la question (début): {}", questionEmbeddingString.substring(0, Math.min(questionEmbeddingString.length(), 60)) + "...");


        // 2. Calculer la distance maximale basée sur le seuil de similarité
        // Similarité cosinus = 1 - Distance cosinus
        // Donc, Distance max = 1 - Similarité min
        double maxDistance = 1.0 - request.getSimilarityThreshold();
        // La distance cosinus est entre 0 (identique) et 2 (opposé). Une distance < 1 signifie une similarité > 0.
        // Assurer que maxDistance est raisonnable (ex: ne pas chercher des vecteurs opposés si seuil est bas)
        if (maxDistance < 0) maxDistance = 0; // Similarité de 1.0 ou plus -> distance de 0 ou moins
        // if (maxDistance > 1) maxDistance = 1; // Optionnel: Si on ne veut que les vecteurs avec similarité positive

        log.debug("Calcul de maxDistance: {} (pour similarityThreshold: {})", maxDistance, request.getSimilarityThreshold());


        // 3. Appeler le Repository
        List<RetrievedChunkProjection> projections = repository.findSimilarChunksByCosineDistance(
                questionEmbeddingString,
                maxDistance,
                request.getTopK()
        );
        log.info("{} chunks trouvés par la requête de similarité.", projections.size());

        // 4. Mapper les projections en DTOs de résultat
        List<RetrievedChunk> results = new ArrayList<>();
        for (RetrievedChunkProjection proj : projections) {
            if (proj.getDistance() == null) { // Sécurité
                log.warn("Projection retournée avec une distance nulle pour l'ID {}. Ignorée.", proj.getId());
                continue;
            }
            double similarityScore = 1.0 - proj.getDistance(); // Recalculer la similarité
            results.add(new RetrievedChunk(
                    proj.getId(),
                    proj.getSourceId(),
                    // Gérer le cas où chunkIndex pourrait être null de la projection
                    (proj.getChunkIndex() != null ? proj.getChunkIndex() : -1),
                    proj.getChunkText(),
                    similarityScore
            ));
        }

        log.info("Retour de {} chunks pertinents après mapping.", results.size());
        return new SearchResult(results.size(), results);
    }
}