package com.deloitte.erip.analytics;

import com.deloitte.erip.analytics.dto.AnalystDashboardResponse;
import com.deloitte.erip.analytics.dto.ExecutiveDashboardResponse;
import com.deloitte.erip.asset.AssetRepository;
import com.deloitte.erip.asset.Criticality;
import com.deloitte.erip.compliance.ComplianceFramework;
import com.deloitte.erip.compliance.ComplianceService;
import com.deloitte.erip.event.SecurityEventRepository;
import com.deloitte.erip.incident.IncidentRepository;
import com.deloitte.erip.incident.IncidentStatus;
import com.deloitte.erip.remediation.RemediationRepository;
import com.deloitte.erip.remediation.RemediationStatus;
import com.deloitte.erip.risk.RiskScore;
import com.deloitte.erip.risk.RiskScoreRepository;
import com.deloitte.erip.risk.RiskTier;
import com.deloitte.erip.vulnerability.VulnerabilityRepository;
import com.deloitte.erip.vulnerability.VulnerabilitySeverity;
import com.deloitte.erip.vulnerability.VulnerabilityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final List<IncidentStatus> CLOSED_INCIDENT_STATUSES = List.of(IncidentStatus.RESOLVED, IncidentStatus.CLOSED);

    private final AssetRepository assetRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final IncidentRepository incidentRepository;
    private final SecurityEventRepository securityEventRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final RemediationRepository remediationRepository;
    private final ComplianceService complianceService;

    public AnalystDashboardResponse analystDashboard() {
        Map<String, Long> assetsByCriticality = new LinkedHashMap<>();
        Arrays.stream(Criticality.values()).forEach(c -> assetsByCriticality.put(c.name(), assetRepository.countByCriticality(c)));

        Map<String, Long> openVulnsBySeverity = new LinkedHashMap<>();
        Arrays.stream(VulnerabilitySeverity.values()).forEach(s ->
                openVulnsBySeverity.put(s.name(), vulnerabilityRepository.countBySeverityAndStatusNot(s, VulnerabilityStatus.RESOLVED)));

        long openVulnerabilities = vulnerabilityRepository.countByStatusNot(VulnerabilityStatus.RESOLVED);
        long openIncidents = incidentRepository.countByStatusNotIn(CLOSED_INCIDENT_STATUSES);

        Instant since24h = Instant.now().minus(24, ChronoUnit.HOURS);
        long anomalies24h = securityEventRepository.countByAnomalyTrueAndEventTimeAfter(since24h);
        long totalEvents24h = securityEventRepository.countByEventTimeAfter(since24h);

        List<AnalystDashboardResponse.TopRiskAsset> topRiskAssets = riskScoreRepository.findLatestForAllAssets().stream()
                .sorted(Comparator.comparing(RiskScore::getOverallScore).reversed())
                .limit(10)
                .map(r -> new AnalystDashboardResponse.TopRiskAsset(
                        r.getAsset().getId(), r.getAsset().getName(), r.getOverallScore(), r.getRiskTier().name()))
                .toList();

        List<AnalystDashboardResponse.PendingRemediation> topRemediations = remediationRepository
                .findAllByStatus(RemediationStatus.PENDING, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "priorityScore")))
                .stream()
                .map(r -> new AnalystDashboardResponse.PendingRemediation(
                        r.getId(), r.getVulnerability().getTitle(), r.getPriority().name(), r.getPriorityScore()))
                .toList();

        return new AnalystDashboardResponse(
                assetRepository.count(), assetsByCriticality, openVulnerabilities, openVulnsBySeverity,
                openIncidents, anomalies24h, totalEvents24h, topRiskAssets, topRemediations);
    }

    public ExecutiveDashboardResponse executiveDashboard() {
        List<RiskScore> latestScores = riskScoreRepository.findLatestForAllAssets();

        BigDecimal avgRisk = latestScores.isEmpty() ? BigDecimal.ZERO : average(latestScores.stream()
                .map(RiskScore::getOverallScore).toList());

        Map<String, Long> assetCountByTier = new LinkedHashMap<>();
        Arrays.stream(RiskTier.values()).forEach(tier -> assetCountByTier.put(tier.name(),
                latestScores.stream().filter(r -> r.getRiskTier() == tier).count()));

        Map<String, List<RiskScore>> byBusinessUnit = latestScores.stream()
                .collect(Collectors.groupingBy(r -> r.getAsset().getBusinessUnit()));

        List<ExecutiveDashboardResponse.BusinessUnitRisk> riskByBusinessUnit = byBusinessUnit.entrySet().stream()
                .map(e -> new ExecutiveDashboardResponse.BusinessUnitRisk(
                        e.getKey(),
                        average(e.getValue().stream().map(RiskScore::getOverallScore).toList()),
                        e.getValue().size()))
                .sorted(Comparator.comparing(ExecutiveDashboardResponse.BusinessUnitRisk::averageRiskScore).reversed())
                .toList();

        List<ExecutiveDashboardResponse.CompliancePostureSummary> compliancePosture = Arrays.stream(ComplianceFramework.values())
                .map(fw -> new ExecutiveDashboardResponse.CompliancePostureSummary(
                        fw.name(), complianceService.getPosture(fw, null).compliancePercentage()))
                .toList();

        long openIncidents = incidentRepository.countByStatusNotIn(CLOSED_INCIDENT_STATUSES);
        long criticalOpenVulns = vulnerabilityRepository.countBySeverityAndStatusNot(
                VulnerabilitySeverity.CRITICAL, VulnerabilityStatus.RESOLVED);

        return new ExecutiveDashboardResponse(avgRisk, assetCountByTier, riskByBusinessUnit, compliancePosture,
                openIncidents, criticalOpenVulns);
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }
}
