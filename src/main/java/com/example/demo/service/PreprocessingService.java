package com.example.demo.service; // Assurez-vous que le package est correct

// --- AJOUTS ---
import lombok.extern.slf4j.Slf4j; // Import pour le logging (nécessite Lombok)
import org.slf4j.Logger; // Alternative si vous n'utilisez pas Lombok
import org.slf4j.LoggerFactory; // Alternative si vous n'utilisez pas Lombok
import java.util.Arrays; // Import pour Arrays.stream()
import java.util.stream.Collectors; // Import pour Collectors.toList()
// --- FIN AJOUTS ---

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j // --- AJOUT: Annotation Lombok pour injecter un logger nommé 'log'. Assurez-vous d'avoir la dépendance Lombok.
public class PreprocessingService {

    // --- Optionnel: Si vous n'utilisez pas Lombok (@Slf4j), décommentez ces lignes : ---
    // private static final Logger log = LoggerFactory.getLogger(PreprocessingService.class);
    // -------------------------------------------------------------------------------

    // --- Votre méthode publique existante (INCHANGÉE) ---
    public List<String> processPdf(MultipartFile file, int nbreMot) throws IOException {
        // Extraction du texte du PDF
        String text = extractTextFromPdf(file);
        log.info("Extracted text from PDF: {}", file.getOriginalFilename()); // Ajout log

        // Découpage en chunks avec overlap de mots (utilise toujours l'ancienne méthode)
        List<String> chunks = chunkTextWithOverlap(text, nbreMot);
        log.info("Chunked text using 'chunkTextWithOverlap' strategy. Chunks: {}", chunks.size()); // Ajout log
        return chunks;
    }

    // --- Votre méthode d'extraction (INCHANGÉE) ---
    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        } catch (IOException e) {
            log.error("Failed to extract text from PDF: {}", file.getOriginalFilename(), e); // Ajout log d'erreur
            throw e; // Relancer l'exception après l'avoir loguée
        }
    }

    // --- Votre méthode de chunking existante (INCHANGÉE) ---
    private List<String> chunkTextWithOverlap(String text, int nbreMot) {
        if (text == null || text.isBlank()) {
            log.warn("chunkTextWithOverlap received null or blank text. Returning empty list.");
            return List.of();
        }
        // Découpe le texte en mots (tokens) en utilisant les espaces comme séparateurs
        String[] words = text.split("\\s+");
        List<String> chunks = new ArrayList<>();

        if (nbreMot <= 0) {
            log.warn("chunkTextWithOverlap received nbreMot <= 0. Returning empty list.");
            return List.of(); // Eviter division par zéro ou boucle infinie
        }

        // Calcul de l'overlap (par exemple, 10% du nombre de mots du chunk)
        // Assurer un overlap minimum de 0 et maximum raisonnable (ex: nbreMot - 1)
        int overlap = Math.max(0, nbreMot / 10);
        if (overlap >= nbreMot && nbreMot > 0) {
            overlap = nbreMot - 1; // Eviter un pas négatif ou nul
        }
        int step = nbreMot - overlap;
        if (step <= 0) {
            step = 1; // Fallback pour assurer la progression
            log.warn("Calculated step size was <= 0 in chunkTextWithOverlap. Forcing step=1.");
        }


        // Découper le texte en morceaux avec overlap de mots
        for (int i = 0; i < words.length; i += step) { // Utiliser 'step' ici
            int end = Math.min(i + nbreMot, words.length);
            // Créer un morceau de texte (gestion du cas où subList est vide, bien que peu probable ici)
            List<String> wordSubList = List.of(words).subList(i, end);
            if (!wordSubList.isEmpty()) {
                String chunk = String.join(" ", wordSubList);
                chunks.add(chunk);
            }
            // Important: si end atteint words.length, il faut sortir pour éviter boucle infinie si step=0 (même si on a essayé d'éviter step=0)
            if (end == words.length) {
                break;
            }
        }
        log.debug("chunkTextWithOverlap created {} chunks.", chunks.size());
        return chunks;
    }


    // --- NOUVELLE MÉTHODE AJOUTÉE : Chunking par Paragraphes ---
    /**
     * Découpe le texte en chunks basés sur les paragraphes.
     * Les paragraphes sont généralement séparés par une ou plusieurs lignes vides
     * dans le texte extrait par PDFBox.
     *
     * @param text Le texte complet à découper.
     * @param minParagraphLengthThreshold La longueur minimale (en caractères) pour qu'un paragraphe
     *                                    soit conservé comme chunk. Aide à filtrer les lignes isolées,
     *                                    titres courts, etc. Mettre 0 ou 1 pour tout garder.
     * @return Une liste de chaînes, où chaque chaîne est un paragraphe nettoyé.
     */
    public List<String> chunkTextByParagraph(String text, int minParagraphLengthThreshold) {
        // Vérification initiale du texte en entrée
        if (text == null || text.isBlank()) {
            log.warn("chunkTextByParagraph received null or blank text. Returning empty list.");
            return List.of(); // Retourne une liste immuable vide
        }

        log.debug("Starting paragraph chunking. Min length threshold: {}", minParagraphLengthThreshold);

        // Regex pour splitter par une ou plusieurs lignes vides (ajustez si nécessaire)
        // \\n : newline
        // \\s* : zero ou plus espaces/tabulations
        // \\n+ : une ou plusieurs nouvelles lignes (capturant le saut de paragraphe)
        String[] paragraphs = text.split("\\n\\s*\\n+");
        log.debug("Split text into {} potential paragraphs.", paragraphs.length);

        List<String> chunks = Arrays.stream(paragraphs)
                .map(String::trim) // Enlever les espaces au début/fin (méthode référence)
                // Remplacer les retours à la ligne SIMPLES (\n) à l'intérieur d'un paragraphe par un espace.
                // Ceci est crucial car PDFBox peut insérer des \n à la fin de chaque ligne visuelle.
                .map(paragraph -> paragraph.replaceAll("\\s*\\n\\s*", " "))
                .filter(paragraph -> !paragraph.isEmpty()) // Enlever les paragraphes vides résultants
                .filter(paragraph -> paragraph.length() >= minParagraphLengthThreshold) // Filtrer par longueur minimale
                .collect(Collectors.toList()); // Collecter les résultats dans une nouvelle liste mutable

        if (chunks.isEmpty() && paragraphs.length > 0) {
            log.warn("Paragraph chunking resulted in zero chunks meeting the minimum length threshold ({}), although initial split found {} paragraphs.", minParagraphLengthThreshold, paragraphs.length);
        } else {
            log.info("Chunked text into {} paragraph-based chunks (min length: {}).", chunks.size(), minParagraphLengthThreshold);
        }

        return chunks;
    }
    // --- FIN DE LA NOUVELLE MÉTHODE ---

} // Fin de la classe PreprocessingService