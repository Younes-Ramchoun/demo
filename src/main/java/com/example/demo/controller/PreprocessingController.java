package com.example.demo.controller;

import com.example.demo.service.PreprocessingService;
import com.example.demo.service.EmbeddingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/preprocessing")
public class PreprocessingController {

    private final PreprocessingService preprocessingService;
    private final EmbeddingService embeddingService;

    public PreprocessingController(PreprocessingService preprocessingService, EmbeddingService embeddingService) {
        this.preprocessingService = preprocessingService;
        this.embeddingService = embeddingService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadAndPreprocessPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "nbre_mot", defaultValue = "50") int nbreMot) {
        try {
            // Extraction et découpage du texte en chunks
            List<String> chunks = preprocessingService.processPdf(file, nbreMot);

            // Stocker les embeddings de chaque chunk dans une map
            Map<String, Object> chunksWithEmbeddings = new HashMap<>();
            for (String chunk : chunks) {
                chunksWithEmbeddings.put(chunk, embeddingService.getEmbeddingsFromHuggingFace(chunk));
            }

            return ResponseEntity.ok(chunksWithEmbeddings);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
