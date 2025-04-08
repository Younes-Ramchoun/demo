package com.example.demo.service;

import com.example.demo.dto.ChunkEmbeddingPair;
import com.example.demo.dto.PdfEmbeddingResult;
import com.example.demo.repository.ChunkEmbeddingRepository; // Assurez-vous que cet import est là
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Assurez-vous que cet import est là
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminIndexingService {

    private static final Logger log = LoggerFactory.getLogger(AdminIndexingService.class);

    private final PreprocessingService preprocessingService;
    private final EmbeddingService2 embeddingService2;
    private final EmbeddingPersistenceService persistenceService;
    private final ChunkEmbeddingRepository chunkRepository; // Déjà présent, c'est bien
    private final String EMBEDDING_MODEL_NAME = "nomic-embed-text";

    @Autowired
    public AdminIndexingService(PreprocessingService preprocessingService,
                                EmbeddingService2 embeddingService2,
                                EmbeddingPersistenceService persistenceService,
                                ChunkEmbeddingRepository chunkRepository) { // Déjà présent, c'est bien
        this.preprocessingService = preprocessingService;
        this.embeddingService2 = embeddingService2;
        this.persistenceService = persistenceService;
        this.chunkRepository = chunkRepository;
    }

    // >>> DÉCOMMENTER ET ACTIVER @Transactional <<<
    @Transactional
    public List<Long> processAndSavePdf(MultipartFile file, int chunkSize, String sourceId) throws IOException, RuntimeException {
        log.info("Début de l'indexation transactionnelle pour '{}'", sourceId); // Modifié pour clarifier

        // >>> AJOUTER L'APPEL À DELETE ICI <<<
        log.debug("Admin Indexing - Étape 0: Suppression des anciens enregistrements pour sourceId='{}'", sourceId);
        int deletedCount = chunkRepository.deleteBySourceId(sourceId);
        if (deletedCount > 0) {
            log.info("Admin Indexing - {} anciens enregistrements supprimés pour '{}'.", deletedCount, sourceId);
        } else {
            log.info("Admin Indexing - Aucun ancien enregistrement trouvé pour '{}' à supprimer.", sourceId);
        }
        // >>> FIN DE L'AJOUT <<<

        // 1. Chunking
        log.debug("Admin Indexing - Étape 1: Chunking...");
        List<String> chunks = preprocessingService.processPdf(file, chunkSize);
        log.info("Admin Indexing - PDF '{}' découpé en {} chunks.", sourceId, chunks.size());

        if (chunks.isEmpty()) {
            log.warn("Admin Indexing - Aucun chunk produit pour '{}'.", sourceId);
            return List.of();
        }

        // 2. Embedding
        log.debug("Admin Indexing - Étape 2: Génération des Embeddings...");
        List<float[]> embeddings = embeddingService2.generateEmbeddings(chunks);
        log.info("Admin Indexing - Embeddings générés pour {} chunks.", embeddings.size());

        // 3. Vérifications et Préparation pour la sauvegarde
        if (embeddings.size() != chunks.size()) {
            String errorMsg = String.format("ERREUR CRITIQUE: Incohérence de taille - Chunks: %d, Embeddings: %d pour %s", chunks.size(), embeddings.size(), sourceId);
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
        if (embeddings.isEmpty()) {
            String errorMsg = String.format("ERREUR CRITIQUE: Aucun embedding retourné pour %s", sourceId);
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        int dimensions = embeddings.get(0).length;
        List<ChunkEmbeddingPair> pairs = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            pairs.add(new ChunkEmbeddingPair(i, chunks.get(i), embeddings.get(i)));
        }
        PdfEmbeddingResult intermediateResult = new PdfEmbeddingResult(sourceId, EMBEDDING_MODEL_NAME, dimensions, chunks.size(), pairs);
        log.debug("Admin Indexing - Objet PdfEmbeddingResult intermédiaire créé.");

        // 4. Sauvegarde
        log.debug("Admin Indexing - Étape 3: Appel du service de sauvegarde...");
        List<Long> savedIds = persistenceService.saveEmbeddings(intermediateResult);
        log.info("Admin Indexing - Sauvegarde terminée pour '{}'. {} IDs retournés.", sourceId, savedIds.size());

        return savedIds; // Commit de la transaction (incluant delete + save)
    }
}