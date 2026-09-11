package com.deloitte.erip.compliance;

import com.deloitte.erip.audit.AuditService;
import com.deloitte.erip.common.exception.ResourceNotFoundException;
import com.deloitte.erip.compliance.dto.AssessmentRequest;
import com.deloitte.erip.compliance.dto.ComplianceControlResponse;
import com.deloitte.erip.compliance.dto.CompliancePosture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ComplianceService {

    private final ComplianceControlRepository controlRepository;
    private final ComplianceAssessmentRepository assessmentRepository;
    private final AuditService auditService;

    public List<ComplianceControlResponse> listControls(ComplianceFramework framework, String businessUnit) {
        String bu = businessUnit == null ? "Enterprise" : businessUnit;
        return controlRepository.findAllByFramework(framework).stream()
                .map(control -> {
                    var latest = assessmentRepository
                            .findFirstByControlIdAndBusinessUnitIgnoreCaseOrderByAssessedAtDesc(control.getId(), bu);
                    return new ComplianceControlResponse(
                            control.getId(),
                            control.getFramework(),
                            control.getControlId(),
                            control.getControlName(),
                            control.getControlDescription(),
                            control.getCategory(),
                            latest.map(ComplianceAssessment::getStatus).orElse(ComplianceStatus.NOT_ASSESSED),
                            latest.map(ComplianceAssessment::getAssessedAt).orElse(null));
                })
                .toList();
    }

    @Transactional
    public void submitAssessment(AssessmentRequest request) {
        ComplianceControl control = controlRepository.findById(request.controlId())
                .orElseThrow(() -> ResourceNotFoundException.of("ComplianceControl", request.controlId()));

        ComplianceAssessment assessment = ComplianceAssessment.builder()
                .control(control)
                .businessUnit(request.businessUnit())
                .status(request.status())
                .evidenceSummary(request.evidenceSummary())
                .assessedAt(Instant.now())
                .build();
        assessmentRepository.save(assessment);
        auditService.record("COMPLIANCE_ASSESSED", "COMPLIANCE_CONTROL", control.getId().toString(),
                Map.of("status", request.status().name(), "businessUnit", request.businessUnit()));
    }

    public CompliancePosture getPosture(ComplianceFramework framework, String businessUnit) {
        List<ComplianceControlResponse> controls = listControls(framework, businessUnit);
        int total = controls.size();
        int compliant = (int) controls.stream().filter(c -> c.latestStatus() == ComplianceStatus.COMPLIANT).count();
        int partial = (int) controls.stream().filter(c -> c.latestStatus() == ComplianceStatus.PARTIAL).count();
        int nonCompliant = (int) controls.stream().filter(c -> c.latestStatus() == ComplianceStatus.NON_COMPLIANT).count();
        int notAssessed = (int) controls.stream().filter(c -> c.latestStatus() == ComplianceStatus.NOT_ASSESSED).count();

        double weightedScore = total == 0 ? 0 : (compliant * 1.0 + partial * 0.5) / total * 100.0;
        return new CompliancePosture(framework, total, compliant, partial, nonCompliant, notAssessed,
                BigDecimal.valueOf(weightedScore).setScale(1, RoundingMode.HALF_UP));
    }
}
