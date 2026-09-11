package com.deloitte.erip.remediation;

import com.deloitte.erip.audit.AuditService;
import com.deloitte.erip.common.exception.ResourceNotFoundException;
import com.deloitte.erip.remediation.dto.RemediationResponse;
import com.deloitte.erip.vulnerability.Vulnerability;
import com.deloitte.erip.vulnerability.VulnerabilityCreatedEvent;
import com.deloitte.erip.vulnerability.VulnerabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RemediationService {

    private final RemediationRepository remediationRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final AuditService auditService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVulnerabilityCreated(VulnerabilityCreatedEvent event) {
        try {
            generateFor(event.vulnerabilityId());
        } catch (Exception ex) {
            log.error("Failed to auto-generate remediation recommendation for vulnerability {}", event.vulnerabilityId(), ex);
        }
    }

    /**
     * REQUIRES_NEW is essential here, not just REQUIRED: by the time an AFTER_COMMIT
     * listener fires, Spring's TransactionSynchronizationManager still reports the
     * publisher's (already-committed) transaction as "active" on this thread. A plain
     * @Transactional (REQUIRED) would join that stale, completed synchronization instead
     * of opening a real one, and the writes below would silently never reach the database.
     * REQUIRES_NEW forces a genuinely fresh transaction/connection.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RemediationRecommendation generateFor(UUID vulnerabilityId) {
        Vulnerability vulnerability = vulnerabilityRepository.findById(vulnerabilityId)
                .orElseThrow(() -> ResourceNotFoundException.of("Vulnerability", vulnerabilityId));

        var score = RemediationEngine.priorityScore(vulnerability);
        RemediationRecommendation recommendation = RemediationRecommendation.builder()
                .vulnerability(vulnerability)
                .priority(RemediationEngine.priorityTier(score))
                .priorityScore(score)
                .recommendation(RemediationEngine.buildRecommendationText(vulnerability))
                .estimatedEffortHours(RemediationEngine.estimatedEffortHours(vulnerability))
                .status(RemediationStatus.PENDING)
                .build();

        RemediationRecommendation saved = remediationRepository.save(recommendation);
        auditService.record("REMEDIATION_GENERATED", "REMEDIATION", saved.getId().toString(),
                Map.of("vulnerabilityId", vulnerabilityId.toString(), "priority", saved.getPriority().name()));
        return saved;
    }

    public Page<RemediationResponse> list(RemediationStatus status, RemediationPriority priority, Pageable pageable) {
        Page<RemediationRecommendation> page;
        if (status != null) {
            page = remediationRepository.findAllByStatus(status, pageable);
        } else if (priority != null) {
            page = remediationRepository.findAllByPriority(priority, pageable);
        } else {
            page = remediationRepository.findAllByOrderByPriorityScoreDesc(pageable);
        }
        return page.map(this::toResponse);
    }

    public RemediationResponse updateStatus(UUID id, RemediationStatus status) {
        RemediationRecommendation recommendation = remediationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("RemediationRecommendation", id));
        recommendation.setStatus(status);
        RemediationRecommendation saved = remediationRepository.save(recommendation);
        auditService.record("REMEDIATION_STATUS_CHANGED", "REMEDIATION", id.toString(), Map.of("status", status.name()));
        return toResponse(saved);
    }

    private RemediationResponse toResponse(RemediationRecommendation r) {
        return new RemediationResponse(
                r.getId(),
                r.getVulnerability().getId(),
                r.getVulnerability().getTitle(),
                r.getVulnerability().getAsset().getId(),
                r.getVulnerability().getAsset().getName(),
                r.getPriority(),
                r.getPriorityScore(),
                r.getRecommendation(),
                r.getEstimatedEffortHours(),
                r.getStatus(),
                r.getGeneratedAt());
    }
}
