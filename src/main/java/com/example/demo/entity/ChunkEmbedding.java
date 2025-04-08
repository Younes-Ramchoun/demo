package com.example.demo.entity; // Ou un package approprié pour les entités

import jakarta.persistence.*;
// Importez le type Vector si vous utilisez la dépendance pgvector-spring spécifique
// import com.pgvector.PGvector; // Si vous utilisez l'ancien ou une approche manuelle
// Ou utilisez simplement float[] si Hibernate/Spring Data gère la conversion

import java.time.LocalDateTime;

@Entity
@Table(name = "chunk_embeddings") // Nom de la table dans PostgreSQL
public class ChunkEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT") // Stocke le texte original du chunk
    private String chunkText;

    // --- Le Vecteur d'Embedding ---
    // Option 1: Utiliser float[] (souvent supporté par les intégrations Spring Data JPA pour pgvector)
    // Assurez-vous que la dimension (768 pour nomic-embed-text) est correcte !
    @Column(nullable = false, columnDefinition = "vector(768)")
    private float[] embedding;

    // Option 2: Utiliser com.pgvector.PGvector (si vous gérez manuellement ou utilisez une ancienne intégration)
    // @Column(columnDefinition = "vector(768)")
    // @Convert(converter = PGvectorConverter.class) // Nécessiterait un converter custom
    // private PGvector embedding;

    @Column(nullable = false) // Identifier la source (ex: nom du fichier PDF)
    private String sourceId;

    @Column(nullable = false) // Garder l'ordre des chunks du document original
    private int chunkIndex;

    @Column // Modèle utilisé pour générer l'embedding
    private String modelName = "nomic-embed-text"; // Peut être défini ici ou récupéré dynamiquement

    @Column // Quand l'enregistrement a été créé
    private LocalDateTime createdAt = LocalDateTime.now();

    // --- Constructeurs, Getters, Setters (Lombok @Data peut simplifier) ---

    public ChunkEmbedding() {
    }

    public ChunkEmbedding(String chunkText, float[] embedding, String sourceId, int chunkIndex, String modelName) {
        this.chunkText = chunkText;
        this.embedding = embedding;
        this.sourceId = sourceId;
        this.chunkIndex = chunkIndex;
        this.modelName = modelName;
        this.createdAt = LocalDateTime.now();
    }

    // --- Getters et Setters ---
    // (Générés par Lombok @Data ou manuellement)

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getChunkText() { return chunkText; }
    public void setChunkText(String chunkText) { this.chunkText = chunkText; }
    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}