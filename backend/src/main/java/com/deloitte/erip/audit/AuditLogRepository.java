package com.deloitte.erip.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findAllByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);
    Page<AuditLog> findAllByActorUserId(UUID actorUserId, Pageable pageable);
}
