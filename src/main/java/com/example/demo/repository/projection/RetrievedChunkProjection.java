package com.example.demo.repository.projection;

public interface RetrievedChunkProjection {
    Long getId();
    String getChunkText();
    String getSourceId();
    Integer getChunkIndex(); // Utiliser Integer car la valeur peut être null dans certains cas de projection
    Double getDistance();    // La distance retournée par pgvector
}