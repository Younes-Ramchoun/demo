package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PdfEmbeddingResult {
    private String sourceId; // Nom du fichier PDF traité
    private String modelName; // Modèle utilisé (ex: "nomic-embed-text")
    private int dimensions;   // Dimension des vecteurs (ex: 768)
    private int totalChunks;  // Nombre total de chunks générés
    private List<ChunkEmbeddingPair> chunkEmbeddings; // La liste des paires chunk/embedding
}