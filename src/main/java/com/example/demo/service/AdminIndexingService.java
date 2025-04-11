package com.example.demo.service;

import com.example.demo.dto.ChunkEmbeddingPair;
import com.example.demo.dto.PdfEmbeddingResult;
import com.example.demo.repository.ChunkEmbeddingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final ChunkEmbeddingRepository chunkRepository;
    private final String EMBEDDING_MODEL_NAME = "nomic-embed-text"; // Considérez de mettre ceci dans application.properties

    @Autowired
    public AdminIndexingService(PreprocessingService preprocessingService,
                                EmbeddingService2 embeddingService2,
                                EmbeddingPersistenceService persistenceService,
                                ChunkEmbeddingRepository chunkRepository) {
        this.preprocessingService = preprocessingService;
        this.embeddingService2 = embeddingService2;
        this.persistenceService = persistenceService;
        this.chunkRepository = chunkRepository;
    }

    @Transactional
    // --- MODIFICATION: Ajouter le paramètre 'strategy' ---
    public List<Long> processAndSavePdf(MultipartFile file, int chunkSize, String sourceId, String strategy)
            throws IOException, RuntimeException, IllegalArgumentException { // Ajout IllegalArgumentException

        log.info("Début de l'indexation transactionnelle pour '{}' avec strategy '{}'", sourceId, strategy);

        // 0. Suppression des anciens enregistrements (inchangé)
        log.debug("Admin Indexing - Étape 0: Suppression des anciens enregistrements pour sourceId='{}'", sourceId);
        int deletedCount = chunkRepository.deleteBySourceId(sourceId);
        if (deletedCount > 0) {
            log.info("Admin Indexing - {} anciens enregistrements supprimés pour '{}'.", deletedCount, sourceId);
        } else {
            log.info("Admin Indexing - Aucun ancien enregistrement trouvé pour '{}' à supprimer.", sourceId);
        }

        // --- MODIFICATION MAJEURE: Extraction et Chunking basés sur la stratégie ---
        // A. Extraire le texte une seule fois
        log.debug("Admin Indexing - Étape 1a: Extraction du texte...");
        String text;
        try {
            text = preprocessingService.extractTextFromPdf(file); // Assurez-vous que cette méthode est accessible (elle est private dans votre code original, rendez-la public ou package-private)
        } catch (IOException e) {
            log.error("Erreur lors de l'extraction du texte pour {}", sourceId, e);
            throw e; // Relancer pour être géré par le contrôleur
        }
        log.info("Admin Indexing - Texte extrait pour '{}'.", sourceId);


        // B. Appliquer la stratégie de chunking
        List<String> chunks;
        log.debug("Admin Indexing - Étape 1b: Chunking avec strategy '{}'...", strategy);

        if ("paragraph".equalsIgnoreCase(strategy)) {
            // Utiliser chunkSize comme seuil minimum de longueur de paragraphe
            log.info("Admin Indexing - Application de la stratégie 'paragraph' avec minLength={}", chunkSize);
            chunks = preprocessingService.chunkTextByParagraph(text, chunkSize);

        } else if ("overlap".equalsIgnoreCase(strategy)) {
            // Utiliser chunkSize comme nombre de mots pour l'overlap
            log.info("Admin Indexing - Application de la stratégie 'overlap' avec nbreMot={}", chunkSize);
            // Appel de la méthode rendue publique à l'étape 1
            chunks = preprocessingService.chunkTextWithOverlap(text, chunkSize);

        } else {
            // Sécurité: Si la validation dans le contrôleur échoue ou est retirée
            log.error("Stratégie de chunking non supportée reçue dans le service: '{}'", strategy);
            throw new IllegalArgumentException("Stratégie de chunking non supportée: " + strategy);
        }

        log.info("Admin Indexing - PDF '{}' découpé en {} chunks via la stratégie '{}'.", sourceId, chunks.size(), strategy);
        // --- FIN MODIFICATION MAJEURE ---


        if (chunks.isEmpty()) {
            log.warn("Admin Indexing - Aucun chunk produit pour '{}' avec la stratégie '{}'. Arrêt.", sourceId, strategy);
            return List.of(); // Retourne une liste vide, la transaction sera commit (seulement le delete a eu lieu)
        }

        // 2. Embedding (inchangé)
        log.debug("Admin Indexing - Étape 2: Génération des Embeddings...");
        List<float[]> embeddings = embeddingService2.generateEmbeddings(chunks);
        log.info("Admin Indexing - Embeddings générés pour {} chunks.", embeddings.size());

        // 3. Vérifications et Préparation pour la sauvegarde (inchangé)
        if (embeddings.size() != chunks.size()) {
            String errorMsg = String.format("ERREUR CRITIQUE: Incohérence de taille - Chunks: %d, Embeddings: %d pour %s", chunks.size(), embeddings.size(), sourceId);
            log.error(errorMsg);
            // Provoque un rollback de la transaction (delete inclus)
            throw new RuntimeException(errorMsg);
        }
        // Petite correction ici: si embeddings est vide, chunks l'était aussi, on serait sorti avant.
        // if (embeddings.isEmpty()) { ... } n'est plus nécessaire après la vérif chunks.isEmpty()

        int dimensions = embeddings.isEmpty() ? 0 : embeddings.get(0).length; // Gérer le cas (improbable ici) où embeddings est vide
        List<ChunkEmbeddingPair> pairs = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            pairs.add(new ChunkEmbeddingPair(i, chunks.get(i), embeddings.get(i)));
        }
        PdfEmbeddingResult intermediateResult = new PdfEmbeddingResult(sourceId, EMBEDDING_MODEL_NAME, dimensions, chunks.size(), pairs);
        log.debug("Admin Indexing - Objet PdfEmbeddingResult intermédiaire créé.");

        // 4. Sauvegarde (inchangé)
        log.debug("Admin Indexing - Étape 4: Appel du service de sauvegarde..."); // Étape 3 -> 4 pour cohérence
        List<Long> savedIds = persistenceService.saveEmbeddings(intermediateResult);
        log.info("Admin Indexing - Sauvegarde terminée pour '{}'. {} IDs retournés.", sourceId, savedIds.size());

        // Si tout réussit, la transaction est commit (incluant delete + save)
        return savedIds;
    }
}