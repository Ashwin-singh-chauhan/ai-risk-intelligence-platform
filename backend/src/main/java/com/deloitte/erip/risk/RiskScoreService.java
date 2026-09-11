package com.deloitte.erip.risk;

import com.deloitte.erip.asset.Asset;
import com.deloitte.erip.asset.AssetRepository;
import com.deloitte.erip.audit.AuditService;
import com.deloitte.erip.common.exception.ResourceNotFoundException;
import com.deloitte.erip.compliance.ComplianceFramework;
import com.deloitte.erip.compliance.ComplianceService;
import com.deloitte.erip.event.SecurityEvent;
import com.deloitte.erip.event.SecurityEventRepository;
import com.deloitte.erip.incident.Incident;
import com.deloitte.erip.incident.IncidentRepository;
import com.deloitte.erip.risk.dto.RiskScoreResponse;
import com.deloitte.erip.vulnerability.Vulnerability;
import com.deloitte.erip.vulnerability.VulnerabilityRepository;
import com.deloitte.erip.vulnerability.VulnerabilityStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskScoreService {

    private static final List<VulnerabilityStatus> CLOSED_VULN_STATUSES =
            List.of(VulnerabilityStatus.RESOLVED, VulnerabilityStatus.ACCEPTED_RISK);

    private final AssetRepository assetRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final SecurityEventRepository securityEventRepository;
    private final IncidentRepository incidentRepository;
    private final ComplianceService complianceService;
    private final RiskScoreRepository riskScoreRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public RiskScoreResponse computeForAsset(UUID assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> ResourceNotFoundException.of("Asset", assetId));
        return toResponse(compute(asset));
    }

    @Scheduled(fixedDelayString = "${erip.risk.recompute-interval-ms:900000}", initialDelayString = "30000")
    @Transactional
    public void recomputeAll() {
        List<Asset> assets = assetRepository.findAll();
        log.info("Recomputing enterprise risk scores for {} assets", assets.size());
        for (Asset asset : assets) {
            try {
                compute(asset);
            } catch (Exception ex) {
                log.error("Failed to compute risk score for asset {}", asset.getId(), ex);
            }
        }
    }

    private RiskScore compute(Asset asset) {
        Instant now = Instant.now();

        List<Vulnerability> openVulns = vulnerabilityRepository.findAllByAssetId(asset.getId()).stream()
                .filter(v -> !CLOSED_VULN_STATUSES.contains(v.getStatus()))
                .toList();

        List<SecurityEvent> recentEvents = securityEventRepository
                .findAllByAssetIdAndEventTimeAfter(asset.getId(), now.minus(Duration.ofDays(30)));

        List<Incident> recentIncidents = incidentRepository
                .findAllByRelatedAssetIdAndOpenedAtAfter(asset.getId(), now.minus(Duration.ofDays(180)));

        BigDecimal compliancePercentage = averageCompliancePercentage(asset.getBusinessUnit());

        BigDecimal vulnComponent = RiskEngine.vulnerabilityComponent(openVulns);
        BigDecimal exposureComponent = RiskEngine.exposureComponent(asset);
        BigDecimal threatComponent = RiskEngine.threatActivityComponent(recentEvents, now);
        BigDecimal complianceComponent = RiskEngine.complianceComponent(compliancePercentage);
        BigDecimal incidentComponent = RiskEngine.incidentHistoryComponent(recentIncidents, now);

        BigDecimal overall = RiskEngine.overallScore(vulnComponent, exposureComponent, threatComponent,
                complianceComponent, incidentComponent);
        RiskTier tier = RiskTier.fromScore(overall.doubleValue());

        RiskScore riskScore = RiskScore.builder()
                .asset(asset)
                .overallScore(overall)
                .vulnerabilityComponent(vulnComponent)
                .exposureComponent(exposureComponent)
                .threatActivityComponent(threatComponent)
                .complianceComponent(complianceComponent)
                .incidentHistoryComponent(incidentComponent)
                .riskTier(tier)
                .explanation(buildExplanation(asset, openVulns, recentEvents, recentIncidents, compliancePercentage,
                        vulnComponent, exposureComponent, threatComponent, complianceComponent, incidentComponent))
                .calculatedAt(now)
                .build();

        return riskScoreRepository.save(riskScore);
    }

    private BigDecimal averageCompliancePercentage(String businessUnit) {
        List<BigDecimal> percentages = List.of(
                complianceService.getPosture(ComplianceFramework.NIST_CSF, businessUnit).compliancePercentage(),
                complianceService.getPosture(ComplianceFramework.ISO_27001, businessUnit).compliancePercentage(),
                complianceService.getPosture(ComplianceFramework.SOC2, businessUnit).compliancePercentage());
        double avg = percentages.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(50.0);
        return BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);
    }

    private com.fasterxml.jackson.databind.JsonNode buildExplanation(
            Asset asset, List<Vulnerability> openVulns, List<SecurityEvent> recentEvents,
            List<Incident> recentIncidents, BigDecimal compliancePercentage,
            BigDecimal vulnComponent, BigDecimal exposureComponent, BigDecimal threatComponent,
            BigDecimal complianceComponent, BigDecimal incidentComponent) {

        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode weights = root.putObject("weights");
        weights.put("vulnerability", RiskEngine.WEIGHT_VULNERABILITY.doubleValue());
        weights.put("exposure", RiskEngine.WEIGHT_EXPOSURE.doubleValue());
        weights.put("threatActivity", RiskEngine.WEIGHT_THREAT.doubleValue());
        weights.put("compliance", RiskEngine.WEIGHT_COMPLIANCE.doubleValue());
        weights.put("incidentHistory", RiskEngine.WEIGHT_INCIDENT.doubleValue());

        ObjectNode inputs = root.putObject("inputs");
        inputs.put("openVulnerabilityCount", openVulns.size());
        inputs.put("criticalOpenVulnerabilityCount", (int) openVulns.stream()
                .filter(v -> v.getSeverity().name().equals("CRITICAL")).count());
        inputs.put("recentEventCount30d", recentEvents.size());
        inputs.put("anomalousEventCount30d", (int) recentEvents.stream().filter(SecurityEvent::isAnomaly).count());
        inputs.put("incidentCount180d", recentIncidents.size());
        inputs.put("compliancePercentage", compliancePercentage.doubleValue());
        inputs.put("assetCriticality", asset.getCriticality().name());
        inputs.put("assetExposure", asset.getExposure().name());

        var topVulns = root.putArray("topContributingVulnerabilities");
        openVulns.stream()
                .sorted(Comparator.comparing(Vulnerability::getCvssScore).reversed())
                .limit(3)
                .forEach(v -> {
                    ObjectNode node = topVulns.addObject();
                    node.put("id", v.getId().toString());
                    node.put("cveId", v.getCveId());
                    node.put("title", v.getTitle());
                    node.put("cvssScore", v.getCvssScore().doubleValue());
                    node.put("exploitAvailable", v.isExploitAvailable());
                });

        root.put("narrative", buildNarrative(asset, vulnComponent, exposureComponent, threatComponent,
                complianceComponent, incidentComponent));

        return root;
    }

    private String buildNarrative(Asset asset, BigDecimal vuln, BigDecimal exposure, BigDecimal threat,
                                   BigDecimal compliance, BigDecimal incident) {
        StringBuilder sb = new StringBuilder();
        sb.append(asset.getName()).append(" carries risk primarily driven by ");
        Map<String, BigDecimal> components = Map.of(
                "open vulnerabilities", vuln, "network exposure/criticality", exposure,
                "recent threat activity", threat, "compliance gaps", compliance, "incident history", incident);
        String topDriver = components.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("multiple factors");
        sb.append(topDriver).append(". Vulnerability=").append(vuln)
                .append(", Exposure=").append(exposure)
                .append(", Threat=").append(threat)
                .append(", Compliance=").append(compliance)
                .append(", IncidentHistory=").append(incident).append(" (0-100 scale each).");
        return sb.toString();
    }

    public RiskScoreResponse getLatest(UUID assetId) {
        RiskScore score = riskScoreRepository.findFirstByAssetIdOrderByCalculatedAtDesc(assetId)
                .orElseThrow(() -> ResourceNotFoundException.of("RiskScore for asset", assetId));
        return toResponse(score);
    }

    public Page<RiskScoreResponse> history(UUID assetId, Pageable pageable) {
        return riskScoreRepository.findAllByAssetIdOrderByCalculatedAtDesc(assetId, pageable).map(this::toResponse);
    }

    public List<RiskScoreResponse> latestForAllAssets() {
        return riskScoreRepository.findLatestForAllAssets().stream().map(this::toResponse).toList();
    }

    private RiskScoreResponse toResponse(RiskScore r) {
        return new RiskScoreResponse(
                r.getId(), r.getAsset().getId(), r.getAsset().getName(), r.getOverallScore(), r.getRiskTier(),
                r.getVulnerabilityComponent(), r.getExposureComponent(), r.getThreatActivityComponent(),
                r.getComplianceComponent(), r.getIncidentHistoryComponent(), r.getExplanation(), r.getCalculatedAt());
    }
}
