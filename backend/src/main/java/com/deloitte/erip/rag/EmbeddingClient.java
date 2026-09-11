package com.deloitte.erip.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

/**
 * Calls the Python ML service's embedding endpoint to turn text into a fixed-length vector
 * used for pgvector similarity search. See docs/RAG_FLOW.md for the embedding strategy rationale.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingClient {

    private final WebClient mlServiceWebClient;

    public record EmbeddingRequest(String text) {
    }

    public record EmbeddingResponse(List<Double> embedding, int dimensions) {
    }

    public double[] embed(String text) {
        try {
            EmbeddingResponse response = mlServiceWebClient.post()
                    .uri("/api/v1/embeddings")
                    .bodyValue(new EmbeddingRequest(text))
                    .retrieve()
                    .bodyToMono(EmbeddingResponse.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            if (response == null) {
                return new double[0];
            }
            return response.embedding().stream().mapToDouble(Double::doubleValue).toArray();
        } catch (Exception ex) {
            log.error("Failed to obtain embedding from ML service", ex);
            return new double[0];
        }
    }
}
