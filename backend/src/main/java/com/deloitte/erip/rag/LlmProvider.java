package com.deloitte.erip.rag;

/**
 * Provider-agnostic abstraction over a chat-completion LLM backend. Concrete implementations
 * are selected by the `erip.llm.provider` property, so the assistant can run fully offline
 * (MockLlmProvider) for demos/CI, or be pointed at a real hosted model (OpenAiLlmProvider)
 * without any change to RagAssistantService.
 */
public interface LlmProvider {
    String complete(String systemPrompt, String userPrompt);

    String providerName();
}
