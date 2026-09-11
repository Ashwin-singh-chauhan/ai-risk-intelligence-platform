package com.deloitte.erip.risk;

import com.deloitte.erip.asset.Asset;
import com.deloitte.erip.event.SecurityEvent;
import com.deloitte.erip.incident.Incident;
import com.deloitte.erip.vulnerability.Vulnerability;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Pure, explainable risk-scoring logic. Every weight below is a documented business rule
 * (see docs/RISK_ENGINE.md) rather than a learned parameter, so a score can always be
 * decomposed back into "why" for an analyst or auditor.
 *
 * Overall score (0-100) = weighted sum of five components, each independently 0-100:
 *   30% Vulnerability exposure   - open CVEs weighted by CVSS, exploit and patch availability
 *   20% Asset exposure/criticality - network exposure combined with business criticality
 *   20% Threat activity          - recent security events, weighted by severity and ML anomaly flag, with time decay
 *   15% Compliance impact        - inverse of the asset's business unit's compliance posture
 *   15% Incident history         - past incidents on the asset, weighted by severity, with time decay
 */
public final class RiskEngine {

    public static final BigDecimal WEIGHT_VULNERABILITY = BigDecimal.valueOf(0.30);
    public static final BigDecimal WEIGHT_EXPOSURE = BigDecimal.valueOf(0.20);
    public static final BigDecimal WEIGHT_THREAT = BigDecimal.valueOf(0.20);
    public static final BigDecimal WEIGHT_COMPLIANCE = BigDecimal.valueOf(0.15);
    public static final BigDecimal WEIGHT_INCIDENT = BigDecimal.valueOf(0.15);

    private static final Duration THREAT_WINDOW = Duration.ofDays(30);
    private static final Duration INCIDENT_WINDOW = Duration.ofDays(180);

    private RiskEngine() {
    }

    public static BigDecimal vulnerabilityComponent(List<Vulnerability> openVulnerabilities) {
        if (openVulnerabilities.isEmpty()) {
            return BigDecimal.ZERO;
        }
        double totalWeight = 0.0;
        for (Vulnerability v : openVulnerabilities) {
            double w = v.getCvssScore().doubleValue();
            if (v.isExploitAvailable()) {
                w *= 1.5;
            }
            if (!v.isPatchAvailable()) {
                w *= 1.2;
            }
            totalWeight += w;
        }
        return cap(totalWeight * 3.0);
    }

    public static BigDecimal exposureComponent(Asset asset) {
        double score = asset.getExposure().weight() * 20.0 + asset.getCriticality().weight() * 15.0;
        return cap(score);
    }

    public static BigDecimal threatActivityComponent(List<SecurityEvent> recentEvents, Instant now) {
        if (recentEvents.isEmpty()) {
            return BigDecimal.ZERO;
        }
        double total = 0.0;
        for (SecurityEvent e : recentEvents) {
            double daysAgo = Duration.between(e.getEventTime(), now).toHours() / 24.0;
            double recencyFactor = Math.max(0.2, 1.0 - (daysAgo / THREAT_WINDOW.toDays()));
            double w = e.getSeverity().weight() * recencyFactor;
            if (e.isAnomaly()) {
                w *= 1.5;
            }
            total += w;
        }
        return cap(total * 0.8);
    }

    public static BigDecimal complianceComponent(BigDecimal compliancePercentage) {
        if (compliancePercentage == null) {
            return BigDecimal.valueOf(50);
        }
        double gap = 100.0 - compliancePercentage.doubleValue();
        return cap(gap);
    }

    public static BigDecimal incidentHistoryComponent(List<Incident> incidents, Instant now) {
        if (incidents.isEmpty()) {
            return BigDecimal.ZERO;
        }
        double total = 0.0;
        for (Incident i : incidents) {
            double daysAgo = Duration.between(i.getOpenedAt(), now).toHours() / 24.0;
            double recencyFactor = Math.max(0.15, 1.0 - (daysAgo / INCIDENT_WINDOW.toDays()));
            total += i.getSeverity().weight() * recencyFactor;
        }
        return cap(total * 0.9);
    }

    public static BigDecimal overallScore(BigDecimal vulnerability, BigDecimal exposure, BigDecimal threat,
                                           BigDecimal compliance, BigDecimal incident) {
        BigDecimal weighted = vulnerability.multiply(WEIGHT_VULNERABILITY)
                .add(exposure.multiply(WEIGHT_EXPOSURE))
                .add(threat.multiply(WEIGHT_THREAT))
                .add(compliance.multiply(WEIGHT_COMPLIANCE))
                .add(incident.multiply(WEIGHT_INCIDENT));
        return weighted.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal cap(double value) {
        double clamped = Math.max(0.0, Math.min(100.0, value));
        return BigDecimal.valueOf(clamped).setScale(2, RoundingMode.HALF_UP);
    }
}
