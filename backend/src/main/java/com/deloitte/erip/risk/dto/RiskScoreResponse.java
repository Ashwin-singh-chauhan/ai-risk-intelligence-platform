package com.deloitte.erip.risk.dto;

import com.deloitte.erip.risk.RiskTier;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RiskScoreResponse(
        UUID id,
        UUID assetId,
        String assetName,
        BigDecimal overallScore,
        RiskTier riskTier,
        BigDecimal vulnerabilityComponent,
        BigDecimal exposureComponent,
        BigDecimal threatActivityComponent,
        BigDecimal complianceComponent,
        BigDecimal incidentHistoryComponent,
        JsonNode explanation,
        Instant calculatedAt
) {
}
