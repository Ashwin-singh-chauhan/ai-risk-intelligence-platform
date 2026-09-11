package com.deloitte.erip.remediation;

import com.deloitte.erip.vulnerability.Vulnerability;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure, deterministic scoring logic for turning a vulnerability into a prioritized,
 * human-readable remediation recommendation. Kept side-effect free so it is trivially unit-testable.
 */
public final class RemediationEngine {

    private RemediationEngine() {
    }

    public static BigDecimal priorityScore(Vulnerability vulnerability) {
        double score = vulnerability.getCvssScore().doubleValue() * 10.0;
        score += vulnerability.getAsset().getCriticality().weight() * 12.0;
        score += vulnerability.getAsset().getExposure().weight() * 8.0;
        if (vulnerability.isExploitAvailable()) {
            score += 25.0;
        }
        if (!vulnerability.isPatchAvailable()) {
            score += 10.0;
        }
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    public static RemediationPriority priorityTier(BigDecimal score) {
        double s = score.doubleValue();
        if (s >= 100) return RemediationPriority.URGENT;
        if (s >= 70) return RemediationPriority.HIGH;
        if (s >= 40) return RemediationPriority.MEDIUM;
        return RemediationPriority.LOW;
    }

    public static BigDecimal estimatedEffortHours(Vulnerability vulnerability) {
        return switch (vulnerability.getSeverity()) {
            case CRITICAL -> BigDecimal.valueOf(16);
            case HIGH -> BigDecimal.valueOf(8);
            case MEDIUM -> BigDecimal.valueOf(4);
            case LOW -> BigDecimal.valueOf(2);
        };
    }

    public static String buildRecommendationText(Vulnerability v) {
        StringBuilder sb = new StringBuilder();
        sb.append("Remediate ").append(v.getCveId() != null ? v.getCveId() : "vulnerability")
                .append(" (\"").append(v.getTitle()).append("\") on asset \"").append(v.getAsset().getName())
                .append("\" [").append(v.getAsset().getCriticality()).append(" criticality, ")
                .append(v.getAsset().getExposure()).append(" exposure]. ");

        if (v.isExploitAvailable()) {
            sb.append("A known exploit is available in the wild - treat as an emergency change and patch or compensate immediately. ");
        }
        if (v.isPatchAvailable()) {
            sb.append("A vendor patch is available; schedule deployment through the standard change window ");
            sb.append(v.getAsset().getCriticality().name().equals("CRITICAL") ? "on an expedited basis." : "within the next maintenance cycle.");
        } else {
            sb.append("No vendor patch is currently available; apply compensating controls such as network segmentation, WAF/IPS virtual patching, or temporary service isolation.");
        }
        if (v.getAsset().getExposure().name().equals("PUBLIC")) {
            sb.append(" Because this asset is internet-facing, prioritize ahead of internally-scoped findings of similar severity.");
        }
        return sb.toString();
    }
}
