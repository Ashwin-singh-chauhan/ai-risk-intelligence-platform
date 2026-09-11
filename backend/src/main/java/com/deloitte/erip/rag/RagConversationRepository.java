package com.deloitte.erip.rag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RagConversationRepository extends JpaRepository<RagConversation, UUID> {
    Page<RagConversation> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
