package com.deloitte.erip.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Real LLM backend calling any OpenAI-compatible chat completions API (OpenAI itself, or
 * a compatible gateway/proxy). Activated with erip.llm.provider=openai and erip.llm.api-key.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "erip.llm.provider", havingValue = "openai")
public class OpenAiLlmProvider implements LlmProvider {

    private final WebClient webClient;
    private final String model;

    public OpenAiLlmProvider(
            @Value("${erip.llm.api-key}") String apiKey,
            @Value("${erip.llm.model}") String model,
            @Value("${erip.llm.base-url:https://api.openai.com/v1}") String baseUrl) {
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    private record ChatMessage(String role, String content) {
    }

    private record ChatRequest(String model, List<ChatMessage> messages, double temperature) {
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        try {
            ChatRequest request = new ChatRequest(model, List.of(
                    new ChatMessage("system", systemPrompt),
                    new ChatMessage("user", userPrompt)), 0.2);

            Map<?, ?> response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();

            if (response == null) {
                return "The AI assistant is temporarily unavailable.";
            }
            List<?> choices = (List<?>) response.get("choices");
            Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
            return String.valueOf(message.get("content"));
        } catch (Exception ex) {
            log.error("OpenAI-compatible completion call failed", ex);
            return "The AI assistant is temporarily unavailable. Please try again shortly.";
        }
    }

    @Override
    public String providerName() {
        return "openai:" + model;
    }
}
