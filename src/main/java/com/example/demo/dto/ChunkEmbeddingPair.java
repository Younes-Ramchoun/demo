package com.example.demo.dto; // Ou un package DTO approprié

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkEmbeddingPair {
    private int chunkIndex;
    private String chunkText;
    private float[] embedding; // Correspond au type de l'entité
}