package com.example.demo.controller;

import com.example.demo.service.EmbeddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    @Autowired
    public EmbeddingController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @GetMapping("/get-embeddings")
    public List<Object> getEmbeddings(@RequestParam String text) {
        // Récupérer les embeddings via le service
        return Collections.singletonList(embeddingService.getEmbeddingsFromHuggingFace(text));
    }
}
