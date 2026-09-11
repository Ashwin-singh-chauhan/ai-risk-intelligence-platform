package com.deloitte.erip.analytics.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AnalystDashboardResponse(
        long totalAssets,
        Map<String, Long> assetsByCriticality,
        long openVulnerabilities,
        Map<String, Long> openVulnerabilitiesBySeverity,
        long openIncidents,
        long anomalyEventsLast24h,
        long totalEventsLast24h,
        List<TopRiskAsset> topRiskAssets,
        List<PendingRemediation> topRemediations
) {
    public record TopRiskAsset(UUID assetId, String assetName, BigDecimal overallScore, String riskTier) {
    }

    public record PendingRemediation(UUID id, String vulnerabilityTitle, String priority, BigDecimal priorityScore) {
    }
}
