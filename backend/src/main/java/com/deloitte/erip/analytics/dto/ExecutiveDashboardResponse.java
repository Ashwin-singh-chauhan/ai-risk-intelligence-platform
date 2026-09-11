package com.deloitte.erip.analytics.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ExecutiveDashboardResponse(
        BigDecimal enterpriseAverageRiskScore,
        Map<String, Long> assetCountByRiskTier,
        List<BusinessUnitRisk> riskByBusinessUnit,
        List<CompliancePostureSummary> compliancePosture,
        long openIncidents,
        long criticalOpenVulnerabilities
) {
    public record BusinessUnitRisk(String businessUnit, BigDecimal averageRiskScore, long assetCount) {
    }

    public record CompliancePostureSummary(String framework, BigDecimal compliancePercentage) {
    }
}
