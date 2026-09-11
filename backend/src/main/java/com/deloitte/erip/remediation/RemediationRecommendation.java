package com.deloitte.erip.remediation;

import com.deloitte.erip.vulnerability.Vulnerability;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "remediation_recommendations")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class RemediationRecommendation {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vulnerability_id", nullable = false)
    private Vulnerability vulnerability;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RemediationPriority priority;

    @Column(name = "priority_score", nullable = false)
    private BigDecimal priorityScore;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "estimated_effort_hours")
    private BigDecimal estimatedEffortHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RemediationStatus status = RemediationStatus.PENDING;

    @CreatedDate
    @Column(name = "generated_at", updatable = false)
    private Instant generatedAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
