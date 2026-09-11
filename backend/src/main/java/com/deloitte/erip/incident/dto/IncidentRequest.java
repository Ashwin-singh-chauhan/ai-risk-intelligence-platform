package com.deloitte.erip.incident.dto;

import com.deloitte.erip.incident.IncidentSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record IncidentRequest(
        @NotBlank String title,
        String description,
        @NotNull IncidentSeverity severity,
        UUID relatedAssetId,
        UUID relatedVulnerabilityId,
        UUID relatedEventId
) {
}
