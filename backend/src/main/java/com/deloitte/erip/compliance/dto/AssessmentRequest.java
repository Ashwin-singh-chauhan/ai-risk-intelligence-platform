package com.deloitte.erip.compliance.dto;

import com.deloitte.erip.compliance.ComplianceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssessmentRequest(
        @NotNull UUID controlId,
        @NotBlank String businessUnit,
        @NotNull ComplianceStatus status,
        String evidenceSummary
) {
}
