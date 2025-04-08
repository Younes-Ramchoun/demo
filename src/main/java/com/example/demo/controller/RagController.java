package com.example.demo.controller;

import com.example.demo.dto.RagQueryRequest;
import com.example.demo.dto.RagQueryResponse;
import com.example.demo.service.RagService; // Le service que nous venons de créer

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Collections;
// ... imports (RagQueryRequest, RagQueryResponse, RagService, Logger, etc.) ...

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    private final RagService ragService; // Injecte le RagService (maintenant modifié)

    @Autowired
    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/query")
    public ResponseEntity<RagQueryResponse> query(@RequestBody RagQueryRequest request) {
        // ... Validation ...
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La question ('question') est requise.");
        }

        log.info("Requête RAG (Ollama local) reçue : '{}'", request.getQuestion());
        try {
            // Appel au service RAG mis à jour
            RagQueryResponse response = ragService.query(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // ... Gestion erreur ...
            log.error("Erreur lors du traitement de la requête RAG (Ollama local) pour '{}': {}", request.getQuestion(), e.getMessage(), e);
            RagQueryResponse errorResponse = new RagQueryResponse("Une erreur interne est survenue lors de la génération de la réponse.", Collections.emptyList());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}