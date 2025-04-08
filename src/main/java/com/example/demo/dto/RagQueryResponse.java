package com.example.demo.dto; // Ou votre package DTO

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagQueryResponse {
    private String answer; // La réponse textuelle de DeepInfra
    private List<RetrievedChunk> retrievedSources; // La liste des chunks utilisés comme contexte
}