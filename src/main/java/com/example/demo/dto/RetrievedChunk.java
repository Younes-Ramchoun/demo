package com.example.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedChunk {
    private Long id; // ID de l'enregistrement en base
    private String sourceId; // Nom du fichier source
    private int chunkIndex; // Index du chunk dans le fichier
    private String chunkText; // Texte du chunk récupéré
    private double similarityScore; // Score de similarité (1 - distance)
}