package com.deloitte.erip.incident;

import com.deloitte.erip.asset.Asset;
import com.deloitte.erip.common.BaseEntity;
import com.deloitte.erip.event.SecurityEvent;
import com.deloitte.erip.user.User;
import com.deloitte.erip.vulnerability.Vulnerability;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "incidents")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident extends BaseEntity {

    @Column(name = "incident_number", nullable = false, unique = true)
    private String incidentNumber;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_asset_id")
    private Asset relatedAsset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_vulnerability_id")
    private Vulnerability relatedVulnerability;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_event_id")
    private SecurityEvent relatedEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_analyst_id")
    private User assignedAnalyst;

    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    @Column(name = "resolution_summary", columnDefinition = "TEXT")
    private String resolutionSummary;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "contained_at")
    private Instant containedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;
}
