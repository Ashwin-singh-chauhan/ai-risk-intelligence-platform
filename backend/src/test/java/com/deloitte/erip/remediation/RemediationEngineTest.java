package com.deloitte.erip.remediation;

import com.deloitte.erip.asset.Asset;
import com.deloitte.erip.asset.AssetEnvironment;
import com.deloitte.erip.asset.AssetType;
import com.deloitte.erip.asset.Criticality;
import com.deloitte.erip.asset.Exposure;
import com.deloitte.erip.vulnerability.Vulnerability;
import com.deloitte.erip.vulnerability.VulnerabilitySeverity;
import com.deloitte.erip.vulnerability.VulnerabilityStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RemediationEngineTest {

    private Vulnerability vulnerability(double cvss, boolean exploit, boolean patch, Criticality criticality, Exposure exposure) {
        Asset asset = Asset.builder()
                .name("payment-gateway-01").assetTag("AST-1").assetType(AssetType.APPLICATION)
                .businessUnit("Payments").criticality(criticality).exposure(exposure)
                .environment(AssetEnvironment.PRODUCTION).build();
        return Vulnerability.builder()
                .asset(asset).title("Remote Code Execution").cveId("CVE-2024-0001")
                .cvssScore(BigDecimal.valueOf(cvss))
                .severity(VulnerabilitySeverity.fromCvss(BigDecimal.valueOf(cvss)))
                .status(VulnerabilityStatus.OPEN)
                .exploitAvailable(exploit).patchAvailable(patch)
                .discoveredAt(Instant.now()).build();
    }

    @Test
    void priorityScore_isHigherForExploitedUnpatchedCriticalAsset() {
        Vulnerability lowRisk = vulnerability(4.0, false, true, Criticality.LOW, Exposure.INTERNAL);
        Vulnerability highRisk = vulnerability(9.8, true, false, Criticality.CRITICAL, Exposure.PUBLIC);

        assertThat(RemediationEngine.priorityScore(highRisk)).isGreaterThan(RemediationEngine.priorityScore(lowRisk));
    }

    @Test
    void priorityTier_mapsScoreToExpectedBand() {
        assertThat(RemediationEngine.priorityTier(BigDecimal.valueOf(20))).isEqualTo(RemediationPriority.LOW);
        assertThat(RemediationEngine.priorityTier(BigDecimal.valueOf(50))).isEqualTo(RemediationPriority.MEDIUM);
        assertThat(RemediationEngine.priorityTier(BigDecimal.valueOf(80))).isEqualTo(RemediationPriority.HIGH);
        assertThat(RemediationEngine.priorityTier(BigDecimal.valueOf(110))).isEqualTo(RemediationPriority.URGENT);
    }

    @Test
    void estimatedEffortHours_scalesWithSeverity() {
        Vulnerability critical = vulnerability(9.5, false, true, Criticality.MEDIUM, Exposure.INTERNAL);
        Vulnerability low = vulnerability(2.0, false, true, Criticality.MEDIUM, Exposure.INTERNAL);

        assertThat(RemediationEngine.estimatedEffortHours(critical))
                .isGreaterThan(RemediationEngine.estimatedEffortHours(low));
    }

    @Test
    void recommendationText_mentionsExploitUrgency_whenExploitAvailable() {
        Vulnerability exploited = vulnerability(9.0, true, true, Criticality.HIGH, Exposure.PUBLIC);
        String text = RemediationEngine.buildRecommendationText(exploited);

        assertThat(text).containsIgnoringCase("exploit").containsIgnoringCase("emergency");
    }

    @Test
    void recommendationText_suggestsCompensatingControls_whenNoPatchAvailable() {
        Vulnerability noPatch = vulnerability(7.5, false, false, Criticality.MEDIUM, Exposure.DMZ);
        String text = RemediationEngine.buildRecommendationText(noPatch);

        assertThat(text).containsIgnoringCase("compensating control");
    }
}
