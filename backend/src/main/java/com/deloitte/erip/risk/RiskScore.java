package com.deloitte.erip.risk;

import com.deloitte.erip.asset.Asset;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "risk_scores")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskScore {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "overall_score", nullable = false)
    private BigDecimal overallScore;

    @Column(name = "vulnerability_component", nullable = false)
    private BigDecimal vulnerabilityComponent;

    @Column(name = "exposure_component", nullable = false)
    private BigDecimal exposureComponent;

    @Column(name = "threat_activity_component", nullable = false)
    private BigDecimal threatActivityComponent;

    @Column(name = "compliance_component", nullable = false)
    private BigDecimal complianceComponent;

    @Column(name = "incident_history_component", nullable = false)
    private BigDecimal incidentHistoryComponent;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_tier", nullable = false)
    private RiskTier riskTier;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private JsonNode explanation;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;
}
