package com.deloitte.erip.incident;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    long count();

    List<Incident> findAllByRelatedAssetIdAndOpenedAtAfter(UUID assetId, Instant after);

    long countByStatusNotIn(List<IncidentStatus> statuses);

    long countBySeverityAndStatusNotIn(IncidentSeverity severity, List<IncidentStatus> statuses);

    @Query("""
            SELECT i FROM Incident i WHERE
            (:status IS NULL OR i.status = :status) AND
            (:severity IS NULL OR i.severity = :severity) AND
            (:assetId IS NULL OR i.relatedAsset.id = :assetId)
            ORDER BY i.openedAt DESC
            """)
    Page<Incident> search(
            @Param("status") IncidentStatus status,
            @Param("severity") IncidentSeverity severity,
            @Param("assetId") UUID assetId,
            Pageable pageable);
}
