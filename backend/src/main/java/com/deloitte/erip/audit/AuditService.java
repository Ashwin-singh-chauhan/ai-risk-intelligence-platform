package com.deloitte.erip.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Central write-path for the audit trail. Kept dependency-free of business services
 * so any module can log without introducing a circular dependency.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public void record(String action, String entityType, String entityId, Map<String, Object> details) {
        try {
            String actorEmail = currentActorEmail();
            JsonNode detailsNode = details == null ? null : objectMapper.valueToTree(details);
            AuditLog auditLog = AuditLog.builder()
                    .actorEmail(actorEmail)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(detailsNode)
                    .occurredAt(Instant.now())
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception ex) {
            // Auditing must never break the primary business flow.
            log.warn("Failed to persist audit log for action={} entityType={} entityId={}", action, entityType, entityId, ex);
        }
    }

    private String currentActorEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetails userDetails)) {
            return "system";
        }
        return userDetails.getUsername();
    }
}
