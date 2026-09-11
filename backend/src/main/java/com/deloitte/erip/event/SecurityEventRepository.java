package com.deloitte.erip.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, UUID> {

    List<SecurityEvent> findAllByAssetIdAndEventTimeAfter(UUID assetId, Instant after);

    Page<SecurityEvent> findAllByAnomalyTrueOrderByEventTimeDesc(Pageable pageable);

    @Query("""
            SELECT e FROM SecurityEvent e WHERE
            (:assetId IS NULL OR e.asset.id = :assetId) AND
            (:severity IS NULL OR e.severity = :severity) AND
            (:anomalyOnly = FALSE OR e.anomaly = TRUE) AND
            (:eventType IS NULL OR e.eventType = :eventType)
            ORDER BY e.eventTime DESC
            """)
    Page<SecurityEvent> search(
            @Param("assetId") UUID assetId,
            @Param("severity") EventSeverity severity,
            @Param("anomalyOnly") boolean anomalyOnly,
            @Param("eventType") String eventType,
            Pageable pageable);

    long countByEventTimeAfter(Instant after);

    long countByAnomalyTrueAndEventTimeAfter(Instant after);

    long countByAssetIdAndEventTimeAfter(UUID assetId, Instant after);

    long countByAssetIdAndAnomalyTrueAndEventTimeAfter(UUID assetId, Instant after);

    @Query("SELECT e FROM SecurityEvent e WHERE e.scoredAt IS NULL ORDER BY e.ingestedAt ASC")
    List<SecurityEvent> findUnscoredBatch(org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT e.severity as severity, COUNT(e) as cnt FROM SecurityEvent e
            WHERE e.eventTime > :after GROUP BY e.severity
            """)
    List<SeverityCount> countBySeveritySince(@Param("after") Instant after);

    interface SeverityCount {
        EventSeverity getSeverity();
        long getCnt();
    }
}
