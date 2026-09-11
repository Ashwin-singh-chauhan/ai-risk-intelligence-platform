package com.deloitte.erip.incident;

import com.deloitte.erip.common.dto.PageResponse;
import com.deloitte.erip.incident.dto.IncidentRequest;
import com.deloitte.erip.incident.dto.IncidentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@Tag(name = "Incident Management", description = "Security incident lifecycle tracking")
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_ANALYST')")
    @Operation(summary = "Open a new security incident")
    public ResponseEntity<IncidentResponse> create(@Valid @RequestBody IncidentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an incident by id")
    public ResponseEntity<IncidentResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(incidentService.get(id));
    }

    @GetMapping
    @Operation(summary = "Search and paginate incidents")
    public ResponseEntity<PageResponse<IncidentResponse>> search(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentSeverity severity,
            @RequestParam(required = false) UUID assetId,
            Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(incidentService.search(status, severity, assetId, pageable)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_ANALYST')")
    @Operation(summary = "Transition an incident's lifecycle status")
    public ResponseEntity<IncidentResponse> updateStatus(
            @PathVariable UUID id, @RequestParam IncidentStatus status, @RequestParam(required = false) String note) {
        return ResponseEntity.ok(incidentService.updateStatus(id, status, note));
    }
}
