package com.deloitte.erip.rag;

import com.deloitte.erip.audit.AuditService;
import com.deloitte.erip.rag.dto.AskResponse;
import com.deloitte.erip.user.User;
import com.deloitte.erip.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagAssistantService {

    private static final String SYSTEM_PROMPT = """
            You are the AI security assistant for an enterprise Risk Intelligence Platform used by
            security analysts and executives. Answer ONLY using the CONTEXT provided, which is retrieved
            from the platform's live asset, vulnerability, incident and risk-score records. If the context
            does not contain the answer, say so plainly instead of guessing. Be concise and cite the asset,
            CVE, or incident identifiers you relied on.
            """;

    private final EmbeddingClient embeddingClient;
    private final RagDocumentStore ragDocumentStore;
    private final LlmProvider llmProvider;
    private final RagConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public AskResponse ask(String question) {
        double[] queryEmbedding = embeddingClient.embed(question);
        List<RagDocumentMatch> matches = queryEmbedding.length == 0
                ? List.of()
                : ragDocumentStore.findSimilar(queryEmbedding, 5);

        String context = matches.stream()
                .map(m -> "- [%s %s] %s: %s".formatted(m.sourceType(), m.sourceId(), m.title(), m.content()))
                .collect(Collectors.joining("\n"));

        String prompt = "CONTEXT:\n" + context + "\n\nQUESTION:\n" + question;
        String answer = llmProvider.complete(SYSTEM_PROMPT, prompt);

        List<AskResponse.ReferencedDocument> references = matches.stream()
                .map(m -> new AskResponse.ReferencedDocument(m.sourceType(), m.sourceId(), m.title(), round(m.similarity())))
                .toList();

        persistConversation(question, answer, references);

        return new AskResponse(answer, llmProvider.providerName(), references);
    }

    private void persistConversation(String question, String answer, List<AskResponse.ReferencedDocument> references) {
        User user = currentUser();
        if (user == null) {
            return;
        }
        RagConversation conversation = RagConversation.builder()
                .user(user)
                .question(question)
                .answer(answer)
                .referencedDocuments(objectMapper.valueToTree(references))
                .createdAt(Instant.now())
                .build();
        conversationRepository.save(conversation);
        auditService.record("RAG_QUERY", "RAG_CONVERSATION", conversation.getId().toString(),
                Map.of("referencedDocumentCount", references.size()));
    }

    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetails userDetails)) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(userDetails.getUsername()).orElse(null);
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
