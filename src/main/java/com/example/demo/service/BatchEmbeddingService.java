package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BatchEmbeddingService {

    // Injecting the Hugging Face API token and model name
    @Value("${huggingface.api.token}")
    private String huggingFaceApiToken;

    @Value("${huggingface.api.model:sentence-transformers/all-MiniLM-L6-v2}")
    private String embeddingModel;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<double[]> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        String huggingFaceApiUrl = "https://api-inference.huggingface.co/models/" + embeddingModel;

        // Set HTTP headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + huggingFaceApiToken);

        // Process in batches to avoid exceeding rate limits
        int batchSize = 10;
        List<CompletableFuture<double[]>> futures = new ArrayList<>();

        for (int i = 0; i < texts.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, endIndex);

            futures.addAll(batch.stream()
                    .map(text -> CompletableFuture.supplyAsync(() -> {
                        try {
                            // Build request body for one text
                            Map<String, Object> requestBody = new HashMap<>();
                            requestBody.put("inputs", text);
                            requestBody.put("options", Map.of("wait_for_model", true));

                            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                            // Send request to Hugging Face API
                            ResponseEntity<Object> response = restTemplate.exchange(
                                    huggingFaceApiUrl,
                                    HttpMethod.POST,
                                    entity,
                                    Object.class
                            );

                            // Log the full response for inspection
                            log.info("API Response: {}", response);

                            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                                // Inspect the structure of the response to understand it
                                Object responseBody = response.getBody();
                                log.info("Response Body: {}", responseBody);

                                // Assuming response is a List<List<Double>>, extract embeddings
                                if (responseBody instanceof List) {
                                    List<?> responseList = (List<?>) responseBody;
                                    if (!responseList.isEmpty() && responseList.get(0) instanceof List) {
                                        List<Double> embedding = (List<Double>) responseList.get(0);
                                        double[] result = new double[embedding.size()];
                                        for (int j = 0; j < embedding.size(); j++) {
                                            result[j] = embedding.get(j);
                                        }
                                        return result;
                                    }
                                }
                                log.error("Unexpected response format for text: {}", text);
                            } else {
                                log.error("Failed to get embedding for text: {}", text);
                            }

                            // Return empty array if something goes wrong
                            return new double[0];
                        } catch (Exception e) {
                            log.error("Error getting embedding for text: {}", text, e);
                            return new double[0];
                        }
                    }))
                    .collect(Collectors.toList()));

            // Delay between batches to avoid rate limiting
            if (endIndex < texts.size()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Wait for all futures and return non-empty results
        return futures.stream()
                .map(CompletableFuture::join)
                .filter(embedding -> embedding.length > 0)
                .collect(Collectors.toList());
    }
}
