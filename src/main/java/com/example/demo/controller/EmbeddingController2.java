package com.example.demo.controller;

import com.example.demo.service.EmbeddingService2;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/embeddings2")
public class EmbeddingController2 {

    private final EmbeddingService2 embeddingService;

    public EmbeddingController2(EmbeddingService2 embeddingService) {
        this.embeddingService = embeddingService;
    }

    @PostMapping
    public ResponseEntity<TextEmbeddingResponse> getEmbeddings(@RequestBody TextEmbeddingRequest request) {
        List<float[]> embeddings = embeddingService.generateEmbeddings(request.getChunks());
        return ResponseEntity.ok(new TextEmbeddingResponse(embeddings, embeddings.isEmpty() ? 0 : embeddings.get(0).length));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextEmbeddingRequest {
        private List<String> chunks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextEmbeddingResponse {
        private List<float[]> embeddings;
        private int dimensions;
    }
}
