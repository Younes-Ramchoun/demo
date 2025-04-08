package com.example.demo.controller;

// Importer le DTO de réponse, le nouveau service, et les classes nécessaires
import com.example.demo.dto.EmbeddingSaveResponse;
import com.example.demo.service.AdminIndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin") // Chemin de base pour les API admin
public class AdminIngestionController {

    private static final Logger log = LoggerFactory.getLogger(AdminIngestionController.class);

    private final AdminIndexingService adminIndexingService; // Injecter le nouveau service

    @Autowired
    public AdminIngestionController(AdminIndexingService adminIndexingService) {
        this.adminIndexingService = adminIndexingService;
    }

    @PostMapping("/ingest") // Endpoint pour l'ingestion/indexation complète
    public ResponseEntity<EmbeddingSaveResponse> ingestAndIndexPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "chunkSize", defaultValue = "500") int chunkSize) { // Ajuster la valeur par défaut si besoin

        // Validation des entrées
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier PDF ('file') est requis.");
        }
        if (chunkSize <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La taille de chunk ('chunkSize') doit être positive.");
        }

        String sourceId = file.getOriginalFilename();
        log.info("Requête d'ingestion Admin reçue pour le fichier '{}' avec chunkSize={}", sourceId, chunkSize);

        try {
            // Appel au service qui fait tout
            List<Long> savedIds = adminIndexingService.processAndSavePdf(file, chunkSize, sourceId);

            // Construire la réponse de succès en utilisant le DTO existant
            String message = String.format("Indexation complète réussie pour '%s'.", sourceId);
            // Note: Le nombre de chunks traités est la taille de savedIds si tout s'est bien passé
            EmbeddingSaveResponse response = new EmbeddingSaveResponse(
                    message,
                    sourceId,
                    savedIds.size(), // Chunks traités = embeddings sauvegardés
                    savedIds.size(), // Embeddings sauvegardés
                    savedIds
            );
            log.info("Réponse pour l'ingestion Admin de '{}': {}", sourceId, message);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Erreur d'IO lors de l'ingestion Admin pour '{}': {}", sourceId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la lecture ou du traitement du fichier PDF.", e);
        } catch (RuntimeException e) { // Capturer les erreurs levées par AdminIndexingService (ex: incohérence)
            log.error("Erreur Runtime lors de l'ingestion Admin pour '{}': {}", sourceId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du processus d'indexation: " + e.getMessage(), e);
        } catch (Exception e) { // Capturer d'autres erreurs inattendues (ex: DB dans persistenceService)
            log.error("Erreur inattendue lors de l'ingestion Admin pour '{}': {}", sourceId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur inattendue pendant l'indexation.", e);
        }
    }
}