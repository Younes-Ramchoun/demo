package com.example.demo.dto; // Ou votre package DTO

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagQueryRequest {
    private String question;
    // Pas besoin de sessionId, topK, threshold pour l'instant
}