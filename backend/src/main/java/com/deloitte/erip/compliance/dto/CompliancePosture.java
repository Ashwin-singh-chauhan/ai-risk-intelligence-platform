package com.deloitte.erip.compliance.dto;

import com.deloitte.erip.compliance.ComplianceFramework;

import java.math.BigDecimal;

public record CompliancePosture(
        ComplianceFramework framework,
        int totalControls,
        int compliant,
        int partial,
        int nonCompliant,
        int notAssessed,
        BigDecimal compliancePercentage
) {
}
