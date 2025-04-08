package com.example.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {
    private String question;
    private int topK = 5; // Valeur par défaut si non fournie
    // Seuil de SIMILARITÉ (0.0 à 1.0). Plus élevé = plus similaire.
    private double similarityThreshold = 0.7; // Valeur par défaut si non fournie
}