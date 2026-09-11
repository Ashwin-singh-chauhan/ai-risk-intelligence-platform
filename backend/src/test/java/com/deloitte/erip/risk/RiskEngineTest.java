package com.deloitte.erip.risk;

import com.deloitte.erip.asset.Asset;
import com.deloitte.erip.asset.AssetEnvironment;
import com.deloitte.erip.asset.AssetType;
import com.deloitte.erip.asset.Criticality;
import com.deloitte.erip.asset.Exposure;
import com.deloitte.erip.event.EventSeverity;
import com.deloitte.erip.event.SecurityEvent;
import com.deloitte.erip.incident.Incident;
import com.deloitte.erip.incident.IncidentSeverity;
import com.deloitte.erip.incident.IncidentStatus;
import com.deloitte.erip.vulnerability.Vulnerability;
import com.deloitte.erip.vulnerability.VulnerabilitySeverity;
import com.deloitte.erip.vulnerability.VulnerabilityStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskEngineTest {

    private Asset asset(Criticality criticality, Exposure exposure) {
        return Asset.builder()
                .name("test-asset")
                .assetTag("AST-TEST-1")
                .assetType(AssetType.SERVER)
                .businessUnit("Corporate IT")
                .criticality(criticality)
                .exposure(exposure)
                .environment(AssetEnvironment.PRODUCTION)
                .build();
    }

    private Vulnerability vuln(double cvss, boolean exploit, boolean patch, VulnerabilityStatus status) {
        return Vulnerability.builder()
                .title("test-vuln")
                .cvssScore(BigDecimal.valueOf(cvss))
                .severity(VulnerabilitySeverity.fromCvss(BigDecimal.valueOf(cvss)))
                .status(status)
                .exploitAvailable(exploit)
                .patchAvailable(patch)
                .discoveredAt(Instant.now())
                .build();
    }

    @Test
    void vulnerabilityComponent_isZero_whenNoOpenVulnerabilities() {
        assertThat(RiskEngine.vulnerabilityComponent(List.of())).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void vulnerabilityComponent_increasesWithExploitAndMissingPatch() {
        Vulnerability plain = vuln(7.0, false, true, VulnerabilityStatus.OPEN);
        Vulnerability exploitedUnpatched = vuln(7.0, true, false, VulnerabilityStatus.OPEN);

        BigDecimal plainScore = RiskEngine.vulnerabilityComponent(List.of(plain));
        BigDecimal severeScore = RiskEngine.vulnerabilityComponent(List.of(exploitedUnpatched));

        assertThat(severeScore).isGreaterThan(plainScore);
    }

    @Test
    void vulnerabilityComponent_isCappedAt100() {
        List<Vulnerability> manyCritical = List.of(
                vuln(10, true, false, VulnerabilityStatus.OPEN),
                vuln(10, true, false, VulnerabilityStatus.OPEN),
                vuln(10, true, false, VulnerabilityStatus.OPEN));

        assertThat(RiskEngine.vulnerabilityComponent(manyCritical)).isLessThanOrEqualTo(BigDecimal.valueOf(100));
    }

    @Test
    void exposureComponent_isHigherForPublicCriticalAsset() {
        BigDecimal internalLow = RiskEngine.exposureComponent(asset(Criticality.LOW, Exposure.INTERNAL));
        BigDecimal publicCritical = RiskEngine.exposureComponent(asset(Criticality.CRITICAL, Exposure.PUBLIC));

        assertThat(publicCritical).isGreaterThan(internalLow);
        assertThat(publicCritical).isLessThanOrEqualTo(BigDecimal.valueOf(100));
    }

    @Test
    void threatActivityComponent_weightsAnomaliesHigherAndDecaysWithAge() {
        Instant now = Instant.now();
        SecurityEvent recentAnomaly = SecurityEvent.builder()
                .eventType("PORT_SCAN").severity(EventSeverity.HIGH)
                .eventTime(now.minus(1, ChronoUnit.HOURS)).ingestedAt(now).anomaly(true).build();
        SecurityEvent oldNormal = SecurityEvent.builder()
                .eventType("LOGIN_SUCCESS").severity(EventSeverity.HIGH)
                .eventTime(now.minus(29, ChronoUnit.DAYS)).ingestedAt(now).anomaly(false).build();

        BigDecimal recentAnomalyScore = RiskEngine.threatActivityComponent(List.of(recentAnomaly), now);
        BigDecimal oldNormalScore = RiskEngine.threatActivityComponent(List.of(oldNormal), now);

        assertThat(recentAnomalyScore).isGreaterThan(oldNormalScore);
    }

    @Test
    void complianceComponent_isInverseOfCompliancePercentage() {
        assertThat(RiskEngine.complianceComponent(BigDecimal.valueOf(100))).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(RiskEngine.complianceComponent(BigDecimal.valueOf(0))).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(RiskEngine.complianceComponent(null)).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    @Test
    void incidentHistoryComponent_increasesWithSeverityAndRecency() {
        Instant now = Instant.now();
        Incident recentCritical = Incident.builder()
                .incidentNumber("INC-1").title("t").severity(IncidentSeverity.CRITICAL)
                .status(IncidentStatus.OPEN).openedAt(now.minus(1, ChronoUnit.DAYS)).build();
        Incident oldLow = Incident.builder()
                .incidentNumber("INC-2").title("t").severity(IncidentSeverity.LOW)
                .status(IncidentStatus.CLOSED).openedAt(now.minus(179, ChronoUnit.DAYS)).build();

        assertThat(RiskEngine.incidentHistoryComponent(List.of(recentCritical), now))
                .isGreaterThan(RiskEngine.incidentHistoryComponent(List.of(oldLow), now));
    }

    @Test
    void overallScore_isWeightedSumWithinBounds() {
        BigDecimal overall = RiskEngine.overallScore(
                BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100),
                BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        assertThat(overall).isEqualByComparingTo(BigDecimal.valueOf(100));

        BigDecimal zero = RiskEngine.overallScore(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        assertThat(zero).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void riskTier_mapsScoreToCorrectBand() {
        assertThat(RiskTier.fromScore(10)).isEqualTo(RiskTier.LOW);
        assertThat(RiskTier.fromScore(40)).isEqualTo(RiskTier.MEDIUM);
        assertThat(RiskTier.fromScore(65)).isEqualTo(RiskTier.HIGH);
        assertThat(RiskTier.fromScore(90)).isEqualTo(RiskTier.CRITICAL);
    }
}
