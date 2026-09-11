package com.deloitte.erip.rag;

import com.deloitte.erip.rag.dto.AskRequest;
import com.deloitte.erip.rag.dto.AskResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
@Tag(name = "AI Security Assistant", description = "RAG-based assistant grounded in live platform data")
public class RagController {

    private final RagAssistantService ragAssistantService;
    private final RagIndexingService ragIndexingService;

    @PostMapping("/ask")
    @Operation(summary = "Ask the AI security assistant a question, grounded in platform records")
    public ResponseEntity<AskResponse> ask(@Valid @RequestBody AskRequest request) {
        return ResponseEntity.ok(ragAssistantService.ask(request.question()));
    }

    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Force a full reindex of the RAG knowledge base from live platform data")
    public ResponseEntity<Void> reindex() {
        ragIndexingService.reindexAll();
        return ResponseEntity.accepted().build();
    }
}
