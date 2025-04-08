package com.example.demo.controller;

import com.example.demo.dto.PdfEmbeddingResult; // Le DTO qu'il reçoit en entrée
import com.example.demo.service.EmbeddingPersistenceService; // Le service qui sauvegarde
// Importer le DTO de réponse pour la sauvegarde
import com.example.demo.dto.EmbeddingSaveResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/persistence") // Nouveau chemin de base pour la persistance
public class PersistenceController {

    private static final Logger log = LoggerFactory.getLogger(PersistenceController.class);

    private final EmbeddingPersistenceService persistenceService;

    @Autowired
    public PersistenceController(EmbeddingPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @PostMapping("/save-embeddings")
    public ResponseEntity<EmbeddingSaveResponse> saveGeneratedEmbeddings(@RequestBody PdfEmbeddingResult requestBody) {

        if (requestBody == null || requestBody.getChunkEmbeddings() == null || requestBody.getChunkEmbeddings().isEmpty()) {
            log.warn("Requête de sauvegarde reçue avec des données invalides ou vides.");
            // On peut retourner une erreur 400 Bad Request
            EmbeddingSaveResponse errorResponse = new EmbeddingSaveResponse(
                    "Les données fournies pour la sauvegarde sont vides ou invalides.",
                    (requestBody != null ? requestBody.getSourceId() : "Inconnu"),
                    0, 0, List.of());
            return ResponseEntity.badRequest().body(errorResponse);
            // Ou lancer une exception:
            // throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le corps de la requête (PdfEmbeddingResult) ne peut pas être vide.");
        }

        String sourceId = requestBody.getSourceId();
        log.info("Requête reçue pour sauvegarder les embeddings pour la source '{}'. Nombre de chunks: {}", sourceId, requestBody.getTotalChunks());

        try {
            // Appel au service de persistance
            List<Long> savedIds = persistenceService.saveEmbeddings(requestBody);

            // Construire la réponse de succès
            String message = String.format("Sauvegarde réussie pour '%s'.", sourceId);
            EmbeddingSaveResponse response = new EmbeddingSaveResponse(
                    message,
                    sourceId,
                    requestBody.getTotalChunks(),
                    savedIds.size(),
                    savedIds
            );
            log.info(message + " Nombre d'enregistrements sauvegardés: {}", savedIds.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Si EmbeddingPersistenceService lève une exception (par ex. erreur DB non gérée)
            log.error("Erreur lors de l'appel au service de sauvegarde pour '{}': {}", sourceId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne lors de la sauvegarde des embeddings.", e);
        }
    }
}