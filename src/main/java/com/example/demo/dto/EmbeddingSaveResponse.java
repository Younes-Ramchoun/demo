package com.example.demo.dto; // Assurez-vous que le package est correct

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingSaveResponse {
    private String message;
    private String sourceId;
    private int chunksProcessed;
    private int embeddingsSaved;
    private List<Long> savedIds; // Optionnel: retourner les IDs
}