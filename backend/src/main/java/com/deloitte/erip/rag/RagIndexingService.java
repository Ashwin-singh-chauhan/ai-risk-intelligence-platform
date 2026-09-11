package com.deloitte.erip.rag;

import com.deloitte.erip.asset.Asset;
import com.deloitte.erip.asset.AssetRepository;
import com.deloitte.erip.incident.Incident;
import com.deloitte.erip.incident.IncidentRepository;
import com.deloitte.erip.risk.RiskScore;
import com.deloitte.erip.risk.RiskScoreRepository;
import com.deloitte.erip.vulnerability.Vulnerability;
import com.deloitte.erip.vulnerability.VulnerabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Builds the RAG knowledge base by turning live platform records (assets, vulnerabilities,
 * incidents, risk scores) into short natural-language documents, embedding them, and
 * upserting into the pgvector-backed rag_documents table. This is what lets the assistant
 * answer with grounded references to real records instead of hallucinating.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagIndexingService {

    private final AssetRepository assetRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final IncidentRepository incidentRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final EmbeddingClient embeddingClient;
    private final RagDocumentStore ragDocumentStore;

    /**
     * Not read-only: the JPA reads here are read-only, but each index*() call also writes
     * to rag_documents via JdbcTemplate (see RagDocumentStore) within this same transaction,
     * which Postgres rejects under a read-only transaction.
     */
    @Scheduled(fixedDelayString = "${erip.rag.reindex-interval-ms:1800000}", initialDelayString = "60000")
    @Transactional
    public void reindexAll() {
        log.info("Starting full RAG knowledge base reindex");
        assetRepository.findAll().forEach(this::indexAsset);
        vulnerabilityRepository.findAll().forEach(this::indexVulnerability);
        incidentRepository.findAll().forEach(this::indexIncident);
        riskScoreRepository.findLatestForAllAssets().forEach(this::indexRiskScore);
        log.info("RAG reindex complete. Document count: {}", ragDocumentStore.count());
    }

    public void indexAsset(Asset asset) {
        String content = "Asset \"%s\" (tag %s) is a %s in business unit %s, environment %s. "
                .formatted(asset.getName(), asset.getAssetTag(), asset.getAssetType(), asset.getBusinessUnit(), asset.getEnvironment())
                + "Business criticality: %s. Network exposure: %s. Owner: %s.".formatted(
                        asset.getCriticality(), asset.getExposure(), asset.getOwner());
        ragDocumentStore.upsert("ASSET", asset.getId().toString(), asset.getName(), content,
                embeddingClient.embed(content), Map.of("assetTag", asset.getAssetTag()));
    }

    public void indexVulnerability(Vulnerability v) {
        String content = "Vulnerability %s \"%s\" affects asset \"%s\". CVSS score %s (%s severity). Status: %s. "
                .formatted(v.getCveId(), v.getTitle(), v.getAsset().getName(), v.getCvssScore(), v.getSeverity(), v.getStatus())
                + "Exploit available: %s. Patch available: %s. %s".formatted(
                        v.isExploitAvailable(), v.isPatchAvailable(),
                        v.getRemediationNotes() != null ? "Remediation notes: " + v.getRemediationNotes() : "");
        ragDocumentStore.upsert("VULNERABILITY", v.getId().toString(), v.getTitle(), content,
                embeddingClient.embed(content), Map.of("cveId", String.valueOf(v.getCveId())));
    }

    public void indexIncident(Incident incident) {
        String content = "Incident %s \"%s\" (%s severity, status %s). ".formatted(
                incident.getIncidentNumber(), incident.getTitle(), incident.getSeverity(), incident.getStatus())
                + (incident.getRelatedAsset() != null ? "Related asset: " + incident.getRelatedAsset().getName() + ". " : "")
                + (incident.getRootCause() != null ? "Root cause: " + incident.getRootCause() + ". " : "")
                + (incident.getResolutionSummary() != null ? "Resolution: " + incident.getResolutionSummary() : "");
        ragDocumentStore.upsert("INCIDENT", incident.getId().toString(), incident.getTitle(), content,
                embeddingClient.embed(content), Map.of("incidentNumber", incident.getIncidentNumber()));
    }

    public void indexRiskScore(RiskScore riskScore) {
        String content = "Asset \"%s\" has an enterprise risk score of %s (%s tier). Components - vulnerability: %s, "
                .formatted(riskScore.getAsset().getName(), riskScore.getOverallScore(), riskScore.getRiskTier(), riskScore.getVulnerabilityComponent())
                + "exposure: %s, threat activity: %s, compliance: %s, incident history: %s.".formatted(
                        riskScore.getExposureComponent(), riskScore.getThreatActivityComponent(),
                        riskScore.getComplianceComponent(), riskScore.getIncidentHistoryComponent());
        ragDocumentStore.upsert("RISK_SCORE", riskScore.getAsset().getId().toString(),
                "Risk score - " + riskScore.getAsset().getName(), content,
                embeddingClient.embed(content), Map.of("riskTier", riskScore.getRiskTier().name()));
    }
}
