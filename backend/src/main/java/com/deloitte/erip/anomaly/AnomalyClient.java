package com.deloitte.erip.anomaly;

import com.deloitte.erip.anomaly.dto.AnomalyScoreRequest;
import com.deloitte.erip.anomaly.dto.AnomalyScoreResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Optional;

/**
 * Thin client around the Python ML service's Isolation-Forest anomaly detection endpoint.
 * Fails soft: if the ML service is unavailable, ingestion continues without blocking on scoring.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnomalyClient {

    private final WebClient mlServiceWebClient;

    public Optional<AnomalyScoreResponse> score(AnomalyScoreRequest request) {
        try {
            AnomalyScoreResponse response = mlServiceWebClient.post()
                    .uri("/api/v1/anomaly/score")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AnomalyScoreResponse.class)
                    .retryWhen(Retry.backoff(2, Duration.ofMillis(200)))
                    .timeout(Duration.ofSeconds(3))
                    .block();
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("ML anomaly-scoring service unavailable, skipping score for this event: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
