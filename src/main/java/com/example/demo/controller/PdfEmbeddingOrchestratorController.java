package com.example.demo.controller;

import com.example.demo.service.PreprocessingService;
import com.example.demo.service.EmbeddingService2;
// Importez les NOUVEAUX DTOs
import com.example.demo.dto.ChunkEmbeddingPair;
import com.example.demo.dto.PdfEmbeddingResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/orchestration")
public class PdfEmbeddingOrchestratorController {

    private static final Logger log = LoggerFactory.getLogger(PdfEmbeddingOrchestratorController.class);

    private final PreprocessingService preprocessingService;
    private final EmbeddingService2 embeddingService2;
    private final String EMBEDDING_MODEL_NAME = "nomic-embed-text"; // Ou récupéré de la config

    @Autowired
    public PdfEmbeddingOrchestratorController(PreprocessingService preprocessingService,
                                              EmbeddingService2 embeddingService2) {
        this.preprocessingService = preprocessingService;
        this.embeddingService2 = embeddingService2;
    }

    @PostMapping("/pdf-to-embeddings")
    // Modifiez le type de retour pour utiliser le nouveau DTO
    public ResponseEntity<PdfEmbeddingResult> processPdfAndGenerateEmbeddings(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "chunkSize", defaultValue = "50") int chunkSize) {

        // ... (validation du fichier et chunkSize comme avant) ...
        if (file == null || file.isEmpty()) { /* ... */ throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "..."); }
        if (chunkSize <= 0) { /* ... */ throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "..."); }

        String sourceId = file.getOriginalFilename(); // Utilise le nom du fichier comme sourceId
        log.info("Requête reçue pour traiter le fichier '{}' avec chunkSize={}", sourceId, chunkSize);

        try {
            // --- Étape 1: Chunking ---
            log.debug("Appel de PreprocessingService.processPdf...");
            List<String> chunks = preprocessingService.processPdf(file, chunkSize);
            log.info("PDF découpé en {} chunks.", chunks.size());

            if (chunks.isEmpty()) {
                log.warn("Aucun chunk produit pour {}.", sourceId);
                // Retourner un résultat vide mais structuré
                return ResponseEntity.ok(new PdfEmbeddingResult(sourceId, EMBEDDING_MODEL_NAME, 0, 0, List.of()));
            }

            // --- Étape 2: Génération des Embeddings ---
            log.debug("Appel de EmbeddingService2.generateEmbeddings pour {} chunks...", chunks.size());
            List<float[]> embeddings = embeddingService2.generateEmbeddings(chunks);
            log.info("Embeddings générés pour {} chunks.", embeddings.size());

            // --- Vérifications de cohérence ---
            if (embeddings.size() != chunks.size()) {
                log.error("ERREUR CRITIQUE: Incohérence de taille entre chunks ({}) et embeddings ({}) pour {}", chunks.size(), embeddings.size(), sourceId);
                // C'est une erreur grave, car on ne peut pas associer correctement
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Incohérence interne entre le nombre de chunks et d'embeddings générés.");
            }
            if (embeddings.isEmpty() && !chunks.isEmpty()) {
                log.error("ERREUR CRITIQUE: Aucun embedding retourné par EmbeddingService2 pour {}", sourceId);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne lors de la génération des embeddings via Ollama.");
            }

            // --- Étape 3: Création des Paires Chunk/Embedding ---
            int dimensions = embeddings.isEmpty() ? 0 : embeddings.get(0).length;
            List<ChunkEmbeddingPair> pairs = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                pairs.add(new ChunkEmbeddingPair(
                        i,                 // chunkIndex
                        chunks.get(i),     // chunkText
                        embeddings.get(i)  // embedding (float[])
                ));
            }

            // --- Étape 4: Construction de la Réponse Finale ---
            PdfEmbeddingResult response = new PdfEmbeddingResult(
                    sourceId,
                    EMBEDDING_MODEL_NAME,
                    dimensions,
                    chunks.size(), // totalChunks
                    pairs          // chunkEmbeddings (la liste des paires)
            );

            log.info("Traitement complet réussi pour '{}'.", sourceId);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Erreur d'IO lors du traitement du PDF '{}': {}", sourceId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la lecture ou du traitement du fichier PDF.", e);
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'orchestration pour le fichier '{}': {}", sourceId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur inattendue pendant le traitement.", e);
        }
    }
}