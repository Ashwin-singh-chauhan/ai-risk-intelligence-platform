package com.deloitte.erip.risk;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskScoreRepository extends JpaRepository<RiskScore, UUID> {

    Optional<RiskScore> findFirstByAssetIdOrderByCalculatedAtDesc(UUID assetId);

    Page<RiskScore> findAllByAssetIdOrderByCalculatedAtDesc(UUID assetId, Pageable pageable);

    List<RiskScore> findAllByCalculatedAtAfter(Instant after);

    @Query("""
            SELECT r FROM RiskScore r JOIN FETCH r.asset WHERE r.calculatedAt = (
                SELECT MAX(r2.calculatedAt) FROM RiskScore r2 WHERE r2.asset.id = r.asset.id
            )
            """)
    List<RiskScore> findLatestForAllAssets();

    @Query("""
            SELECT r FROM RiskScore r WHERE r.riskTier = :tier AND r.calculatedAt = (
                SELECT MAX(r2.calculatedAt) FROM RiskScore r2 WHERE r2.asset.id = r.asset.id
            )
            """)
    List<RiskScore> findLatestByTier(@Param("tier") RiskTier tier);
}
