package com.deloitte.erip.rag;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Deterministic, dependency-free LLM stand-in used when erip.llm.provider=mock (the default).
 * It performs simple extractive summarization over the retrieved context so the assistant is
 * fully demoable offline/in CI without any external API key.
 */
@Component
@ConditionalOnProperty(name = "erip.llm.provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmProvider implements LlmProvider {

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return "[Grounded answer synthesized from platform data]\n\n" + extractiveSummary(userPrompt);
    }

    private String extractiveSummary(String prompt) {
        int contextStart = prompt.indexOf("CONTEXT:");
        int questionStart = prompt.indexOf("QUESTION:");
        String context = contextStart >= 0
                ? prompt.substring(contextStart, questionStart >= 0 ? questionStart : prompt.length())
                : prompt;
        String question = questionStart >= 0 ? prompt.substring(questionStart) : "";

        String trimmedContext = context.replace("CONTEXT:", "").trim();
        if (trimmedContext.isBlank()) {
            return "I could not find any grounded records in the platform to answer this question. "
                    + "Try asking about a specific asset, vulnerability, incident, or compliance control.";
        }
        return "Based on the referenced platform records below, here is a summary relevant to your question "
                + "(" + question.replace("QUESTION:", "").trim() + "):\n\n" + trimmedContext;
    }

    @Override
    public String providerName() {
        return "mock";
    }
}
