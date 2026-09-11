package com.deloitte.erip.rag;

import java.util.UUID;

public record RagDocumentMatch(
        UUID id,
        String sourceType,
        String sourceId,
        String title,
        String content,
        double similarity
) {
}
