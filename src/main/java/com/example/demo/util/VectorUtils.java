package com.example.demo.util;

import java.util.stream.Collectors;
import java.util.stream.IntStream; // Importer IntStream

public class VectorUtils {

    /**
     * Convertit un tableau de float en sa représentation textuelle
     * pour pgvector. Ex: [0.1f, -0.2f] -> "[0.1,-0.2]"
     */
    public static String floatArrayToString(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]"; // Vecteur vide
        }

        // CORRECTION: Utiliser IntStream pour itérer sur les indices,
        // puis mapper chaque indice à la valeur string correspondante du tableau.
        return IntStream.range(0, vector.length) // Crée un stream d'entiers: 0, 1, 2, ..., length-1
                .mapToObj(index -> Float.toString(vector[index])) // Pour chaque index, obtient le float et le convertit en String
                .collect(Collectors.joining(",", "[", "]")); // Joint les Strings avec des virgules, et ajoute les crochets
    }
}