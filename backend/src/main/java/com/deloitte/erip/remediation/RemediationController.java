package com.deloitte.erip.remediation;

import com.deloitte.erip.common.dto.PageResponse;
import com.deloitte.erip.remediation.dto.RemediationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/remediations")
@RequiredArgsConstructor
@Tag(name = "Remediation Recommendations", description = "Prioritized, auto-generated remediation guidance")
public class RemediationController {

    private final RemediationService remediationService;

    @GetMapping
    @Operation(summary = "List remediation recommendations ranked by priority score")
    public ResponseEntity<PageResponse<RemediationResponse>> list(
            @RequestParam(required = false) RemediationStatus status,
            @RequestParam(required = false) RemediationPriority priority,
            Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(remediationService.list(status, priority, pageable)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_ANALYST')")
    @Operation(summary = "Update the workflow status of a remediation recommendation")
    public ResponseEntity<RemediationResponse> updateStatus(@PathVariable UUID id, @RequestParam RemediationStatus status) {
        return ResponseEntity.ok(remediationService.updateStatus(id, status));
    }
}
