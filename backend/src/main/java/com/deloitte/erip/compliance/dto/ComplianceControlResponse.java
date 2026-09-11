package com.deloitte.erip.compliance.dto;

import com.deloitte.erip.compliance.ComplianceFramework;
import com.deloitte.erip.compliance.ComplianceStatus;

import java.time.Instant;
import java.util.UUID;

public record ComplianceControlResponse(
        UUID controlId,
        ComplianceFramework framework,
        String controlIdentifier,
        String controlName,
        String controlDescription,
        String category,
        ComplianceStatus latestStatus,
        Instant lastAssessedAt
) {
}
