package com.deloitte.erip.compliance;

import com.deloitte.erip.compliance.dto.AssessmentRequest;
import com.deloitte.erip.compliance.dto.ComplianceControlResponse;
import com.deloitte.erip.compliance.dto.CompliancePosture;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/compliance")
@RequiredArgsConstructor
@Tag(name = "Compliance", description = "Conceptual NIST CSF / ISO 27001 / SOC 2 control mappings and posture")
public class ComplianceController {

    private final ComplianceService complianceService;

    @GetMapping("/controls")
    @Operation(summary = "List controls for a framework with their latest assessed status")
    public ResponseEntity<List<ComplianceControlResponse>> listControls(
            @RequestParam ComplianceFramework framework,
            @RequestParam(required = false) String businessUnit) {
        return ResponseEntity.ok(complianceService.listControls(framework, businessUnit));
    }

    @PostMapping("/assessments")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_ANALYST')")
    @Operation(summary = "Record a new compliance assessment for a control")
    public ResponseEntity<Void> submitAssessment(@Valid @RequestBody AssessmentRequest request) {
        complianceService.submitAssessment(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/posture")
    @Operation(summary = "Aggregate compliance posture percentage for a framework")
    public ResponseEntity<CompliancePosture> getPosture(
            @RequestParam ComplianceFramework framework,
            @RequestParam(required = false) String businessUnit) {
        return ResponseEntity.ok(complianceService.getPosture(framework, businessUnit));
    }
}
