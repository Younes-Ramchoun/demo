package com.example.demo.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PreprocessingService {

    public List<String> processPdf(MultipartFile file, int nbreMot) throws IOException {
        // Extraction du texte du PDF
        String text = extractTextFromPdf(file);

        // Découpage en chunks avec overlap de mots
        return chunkTextWithOverlap(text, nbreMot);
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);  // Extrait le texte du PDF
        }
    }

    private List<String> chunkTextWithOverlap(String text, int nbreMot) {
        // Découpe le texte en mots (tokens) en utilisant les espaces comme séparateurs
        String[] words = text.split("\\s+");
        List<String> chunks = new ArrayList<>();

        // Calcul de l'overlap (par exemple, 10% du nombre de mots du chunk)
        int overlap = nbreMot / 10;  // L'overlap est défini comme 10% de `nbreMot`

        // Découper le texte en morceaux avec overlap de mots
        for (int i = 0; i < words.length; i += (nbreMot - overlap)) {
            int end = Math.min(i + nbreMot, words.length);  // Limite pour ne pas dépasser la longueur du texte
            String chunk = String.join(" ", List.of(words).subList(i, end));  // Crée un morceau de texte
            chunks.add(chunk);
        }

        return chunks;
    }
}
