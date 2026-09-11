package com.deloitte.erip.event;

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
@Table(name = "security_events")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityEvent {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventSeverity severity;

    @Column(name = "source_ip")
    private String sourceIp;

    @Column(name = "destination_ip")
    private String destinationIp;

    @Column(name = "source_port")
    private Integer sourcePort;

    @Column(name = "destination_port")
    private Integer destinationPort;

    private String protocol;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload")
    private JsonNode rawPayload;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    @Column(name = "anomaly_score")
    private BigDecimal anomalyScore;

    @Column(name = "is_anomaly", nullable = false)
    @Builder.Default
    private boolean anomaly = false;

    @Column(name = "scored_at")
    private Instant scoredAt;
}
