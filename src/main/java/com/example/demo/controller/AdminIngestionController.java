package com.example.demo.controller;

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
import java.util.Set; // <<<--- AJOUTER cet import

@RestController
@RequestMapping("/api/admin")
public class AdminIngestionController {

    private static final Logger log = LoggerFactory.getLogger(AdminIngestionController.class);
    private final AdminIndexingService adminIndexingService;

    // --- AJOUT: Définir les stratégies valides ---
    private static final Set<String> VALID_STRATEGIES = Set.of("overlap", "paragraph");

    @Autowired
    public AdminIngestionController(AdminIndexingService adminIndexingService) {
        this.adminIndexingService = adminIndexingService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<EmbeddingSaveResponse> ingestAndIndexPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "chunkSize", defaultValue = "500") int chunkSize,
            // --- AJOUT: Nouveau paramètre pour la stratégie de chunking ---
            @RequestParam(value = "strategy", defaultValue = "overlap") String strategy) {

        // Validation des entrées
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier PDF ('file') est requis.");
        }
        if (chunkSize <= 0) {
            // --- MODIFICATION: Clarifier le message d'erreur ---
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le paramètre 'chunkSize' doit être positif.");
        }
        // --- AJOUT: Validation de la stratégie ---
        String lowerCaseStrategy = strategy.toLowerCase(); // Comparaison insensible à la casse
        if (!VALID_STRATEGIES.contains(lowerCaseStrategy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stratégie de chunking invalide ('strategy'). Valeurs possibles: " + VALID_STRATEGIES);
        }

        String sourceId = file.getOriginalFilename();
        // --- MODIFICATION: Mettre à jour le log initial ---
        log.info("Requête d'ingestion Admin reçue pour '{}' avec chunkSize={} et strategy='{}'", sourceId, chunkSize, lowerCaseStrategy);

        try {
            // --- MODIFICATION: Passer la stratégie au service ---
            List<Long> savedIds = adminIndexingService.processAndSavePdf(file, chunkSize, sourceId, lowerCaseStrategy);

            // Construire la réponse de succès (reste inchangé)
            String message = String.format("Indexation (%s strategy) complète réussie pour '%s'.", lowerCaseStrategy, sourceId);
            EmbeddingSaveResponse response = new EmbeddingSaveResponse(
                    message,
                    sourceId,
                    savedIds.size(),
                    savedIds.size(),
                    savedIds
            );
            log.info("Réponse pour l'ingestion Admin de '{}': {}", sourceId, message);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) { // --- AJOUT: Capturer les erreurs spécifiques (comme stratégie invalide dans le service si non validée ici) ---
            log.warn("Erreur de validation lors de l'ingestion Admin pour '{}': {}", sourceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IOException e) {
            log.error("Erreur d'IO lors de l'ingestion Admin pour '{}': {}", sourceId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la lecture ou du traitement du fichier PDF.", e);
        } catch (RuntimeException e) {
            log.error("Erreur Runtime lors de l'ingestion Admin pour '{}': {}", sourceId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du processus d'indexation: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'ingestion Admin pour '{}': {}", sourceId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur inattendue pendant l'indexation.", e);
        }
    }
}