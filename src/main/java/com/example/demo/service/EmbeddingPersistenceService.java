package com.example.demo.service;

import com.example.demo.dto.ChunkEmbeddingPair;
import com.example.demo.dto.PdfEmbeddingResult;
import com.example.demo.entity.ChunkEmbedding;
import com.example.demo.repository.ChunkEmbeddingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // TRÈS IMPORTANT

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingPersistenceService.class);
    private final ChunkEmbeddingRepository repository;

    @Autowired
    public EmbeddingPersistenceService(ChunkEmbeddingRepository repository) {
        this.repository = repository;
    }

    // L'annotation @Transactional assure que soit TOUT est sauvegardé, soit RIEN (en cas d'erreur)
    @Transactional
    public List<Long> saveEmbeddings(PdfEmbeddingResult result) {
        if (result == null || result.getChunkEmbeddings() == null || result.getChunkEmbeddings().isEmpty()) {
            log.warn("Tentative de sauvegarde d'un résultat vide ou invalide.");
            return List.of();
        }

        List<ChunkEmbedding> entitiesToSave = new ArrayList<>();
        for (ChunkEmbeddingPair pair : result.getChunkEmbeddings()) {
            // Création de l'entité à partir du DTO et des métadonnées
            ChunkEmbedding entity = new ChunkEmbedding(
                    pair.getChunkText(),
                    pair.getEmbedding(),
                    result.getSourceId(),
                    pair.getChunkIndex(),
                    result.getModelName()
                    // createdAt est géré dans le constructeur ou par JPA
            );
            entitiesToSave.add(entity);
        }

        try {
            log.info("Sauvegarde de {} entités ChunkEmbedding pour la source '{}'", entitiesToSave.size(), result.getSourceId());
            // Utilise saveAll pour une meilleure efficacité (moins d'appels DB)
            List<ChunkEmbedding> savedEntities = repository.saveAll(entitiesToSave);
            log.info("{} entités ChunkEmbedding sauvegardées avec succès.", savedEntities.size());

            // Retourne les IDs générés des entités sauvegardées
            return savedEntities.stream().map(ChunkEmbedding::getId).toList();

        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde des embeddings pour la source '{}': {}", result.getSourceId(), e.getMessage(), e);
            // L'exception sera probablement propagée, et @Transactional devrait déclencher un rollback.
            // Vous pourriez vouloir la relancer encapsulée dans une exception personnalisée.
            // throw new PersistenceException("Échec de la sauvegarde des embeddings pour " + result.getSourceId(), e);
            return List.of(); // Ou retourner une liste vide/lever une exception selon la gestion d'erreur souhaitée
        }
    }
}