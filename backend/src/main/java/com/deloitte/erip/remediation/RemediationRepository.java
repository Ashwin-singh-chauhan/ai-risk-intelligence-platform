package com.deloitte.erip.remediation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RemediationRepository extends JpaRepository<RemediationRecommendation, UUID> {
    Page<RemediationRecommendation> findAllByStatus(RemediationStatus status, Pageable pageable);
    Page<RemediationRecommendation> findAllByPriority(RemediationPriority priority, Pageable pageable);
    Optional<RemediationRecommendation> findFirstByVulnerabilityIdOrderByGeneratedAtDesc(UUID vulnerabilityId);
    Page<RemediationRecommendation> findAllByOrderByPriorityScoreDesc(Pageable pageable);
}
