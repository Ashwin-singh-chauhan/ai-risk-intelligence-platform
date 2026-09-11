package com.deloitte.erip.remediation.dto;

import com.deloitte.erip.remediation.RemediationPriority;
import com.deloitte.erip.remediation.RemediationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RemediationResponse(
        UUID id,
        UUID vulnerabilityId,
        String vulnerabilityTitle,
        UUID assetId,
        String assetName,
        RemediationPriority priority,
        BigDecimal priorityScore,
        String recommendation,
        BigDecimal estimatedEffortHours,
        RemediationStatus status,
        Instant generatedAt
) {
}
