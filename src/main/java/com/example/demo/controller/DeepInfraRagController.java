package com.example.demo.controller;

import com.example.demo.dto.RagQueryRequest;
import com.example.demo.dto.RagQueryResponse;
import com.example.demo.service.DeepInfraRagService; // <<< Injecter le NOUVEAU service

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List; // Importer si utilisé

@RestController
@RequestMapping("/api/rag-deepinfra") // <<< NOUVEAU CHEMIN DE BASE
public class DeepInfraRagController { // <<< NOUVEAU NOM

    private static final Logger log = LoggerFactory.getLogger(DeepInfraRagController.class);

    private final DeepInfraRagService deepInfraRagService; // <<< Injecter le NOUVEAU service

    @Autowired
    public DeepInfraRagController(DeepInfraRagService deepInfraRagService) { // <<< Injecter le NOUVEAU service
        this.deepInfraRagService = deepInfraRagService;
    }

    @PostMapping("/query") // <<< Endpoint sous le nouveau chemin de base
    public ResponseEntity<RagQueryResponse> query(@RequestBody RagQueryRequest request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La question ('question') est requise.");
        }

        log.info("Requête RAG (DeepInfra) reçue : '{}'", request.getQuestion());
        try {
            // Appeler la méthode du NOUVEAU service
            RagQueryResponse response = deepInfraRagService.queryWithDeepInfra(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors du traitement de la requête RAG (DeepInfra) pour '{}': {}", request.getQuestion(), e.getMessage(), e);
            RagQueryResponse errorResponse = new RagQueryResponse("Une erreur interne est survenue lors de la génération de la réponse.", Collections.emptyList());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}