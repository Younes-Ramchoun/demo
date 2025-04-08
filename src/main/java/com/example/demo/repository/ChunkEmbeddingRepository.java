package com.example.demo.repository;

import com.example.demo.entity.ChunkEmbedding;
import com.example.demo.repository.projection.RetrievedChunkProjection; // Importer la projection
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChunkEmbeddingRepository extends JpaRepository<ChunkEmbedding, Long> {

    // Méthode pour trouver les chunks similaires en utilisant la distance cosinus
    // Note: L'opérateur <=> mesure la DISTANCE cosinus (0=identique, 1=orthogonal, 2=opposé)
    @Query(nativeQuery = true, value = """
        SELECT
            id,
            chunk_text AS chunkText,
            source_id AS sourceId,
            chunk_index AS chunkIndex,
            (embedding <=> CAST(:questionEmbeddingString AS vector)) AS distance
        FROM
            chunk_embeddings
        WHERE
            -- Filtrer par distance MAXIMALE (seuil de similarité MINIMAL)
            (embedding <=> CAST(:questionEmbeddingString AS vector)) < :maxDistance
        ORDER BY
            distance ASC -- Trier par distance la plus faible (similarité la plus élevée)
        LIMIT :topK -- Limiter au nombre de résultats demandés
    """)
    List<RetrievedChunkProjection> findSimilarChunksByCosineDistance(
            @Param("questionEmbeddingString") String questionEmbeddingString,
            @Param("maxDistance") double maxDistance,
            @Param("topK") int topK
    );

    /**
     * Supprime tous les enregistrements pour un sourceId (nom de fichier) donné.
     */
    @Modifying
    @Transactional // Important pour que l'opération de suppression soit transactionnelle
    @Query("DELETE FROM ChunkEmbedding c WHERE c.sourceId = :sourceId")
    int deleteBySourceId(@Param("sourceId") String sourceId);
}