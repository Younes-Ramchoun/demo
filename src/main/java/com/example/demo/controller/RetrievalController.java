package com.example.demo.controller;

import com.example.demo.dto.SearchRequest;
import com.example.demo.dto.SearchResult;
import com.example.demo.service.RetrievalService; // Le service que nous venons de créer

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;


@RestController
@RequestMapping("/api/retrieval") // Chemin de base pour ce contrôleur
public class RetrievalController {

    private static final Logger log = LoggerFactory.getLogger(RetrievalController.class);

    private final RetrievalService retrievalService;

    @Autowired
    public RetrievalController(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @PostMapping("/search")
    public ResponseEntity<SearchResult> searchSimilarDocuments(@RequestBody SearchRequest request) {
        // Validation des entrées
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La question ('question') est requise.");
        }
        if (request.getTopK() <= 0) {
            log.warn("topK invalide reçu ({}), utilisation de la valeur par défaut 5.", request.getTopK());
            request.setTopK(5); // Ou renvoyer une erreur 400
            // throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le paramètre 'topK' doit être positif.");
        }
        // Validation du seuil (0.0 à 1.0)
        if (request.getSimilarityThreshold() < 0.0 || request.getSimilarityThreshold() > 1.0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le paramètre 'similarityThreshold' doit être entre 0.0 et 1.0.");
        }

        log.info("Requête de recherche reçue: Question='{}', topK={}, threshold={}",
                request.getQuestion(), request.getTopK(), request.getSimilarityThreshold());

        try {
            // Appel au service pour effectuer la recherche
            SearchResult result = retrievalService.searchSimilarChunks(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Capturer les erreurs inattendues du service ou du repository
            log.error("Erreur serveur lors de la recherche pour la question '{}': {}", request.getQuestion(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur est survenue lors de la recherche.", e);
        }
    }
}