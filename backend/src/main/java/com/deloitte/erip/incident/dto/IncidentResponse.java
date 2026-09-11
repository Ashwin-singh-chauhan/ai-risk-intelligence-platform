package com.deloitte.erip.incident.dto;

import com.deloitte.erip.incident.IncidentSeverity;
import com.deloitte.erip.incident.IncidentStatus;

import java.time.Instant;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        String incidentNumber,
        String title,
        String description,
        IncidentSeverity severity,
        IncidentStatus status,
        UUID relatedAssetId,
        String relatedAssetName,
        UUID relatedVulnerabilityId,
        String assignedAnalystName,
        String rootCause,
        String resolutionSummary,
        Instant openedAt,
        Instant containedAt,
        Instant resolvedAt,
        Instant closedAt
) {
}
