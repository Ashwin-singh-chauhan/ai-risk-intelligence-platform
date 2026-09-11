package com.deloitte.erip.rag.dto;

import java.util.List;

public record AskResponse(
        String answer,
        String llmProvider,
        List<ReferencedDocument> referencedDocuments
) {
    public record ReferencedDocument(String sourceType, String sourceId, String title, double similarity) {
    }
}
