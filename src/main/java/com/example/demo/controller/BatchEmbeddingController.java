package com.example.demo.controller;

import com.example.demo.service.BatchEmbeddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/embeddings")
@Slf4j
public class BatchEmbeddingController {

    @Autowired
    private BatchEmbeddingService batchEmbeddingService;

    @Value("${huggingface.api.model:sentence-transformers/all-MiniLM-L6-v2}")
    private String embeddingModel;

    @PostMapping
    public ResponseEntity<?> getEmbeddings(@RequestBody TextEmbeddingRequest request) {
        try {
            log.info("Processing embedding request for {} texts", request.getTexts().size());

            List<double[]> embeddings = batchEmbeddingService.generateEmbeddings(request.getTexts());

            TextEmbeddingResponse response = new TextEmbeddingResponse();
            response.setEmbeddings(embeddings);
            response.setDimensions(embeddings.isEmpty() ? 0 : embeddings.get(0).length);
            response.setModel(embeddingModel);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing embedding request", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        Map<String, String> error = new HashMap<>();
        error.put("error", "Une erreur s'est produite: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextEmbeddingRequest {
        private List<String> texts;
    }

    @Data
    @NoArgsConstructor
    public static class TextEmbeddingResponse {
        private List<double[]> embeddings;
        private int dimensions;
        private String model;
    }
}