package com.deloitte.erip.incident;

import com.deloitte.erip.asset.Asset;
import com.deloitte.erip.asset.AssetRepository;
import com.deloitte.erip.audit.AuditService;
import com.deloitte.erip.common.exception.ResourceNotFoundException;
import com.deloitte.erip.event.SecurityEvent;
import com.deloitte.erip.event.SecurityEventRepository;
import com.deloitte.erip.incident.dto.IncidentRequest;
import com.deloitte.erip.incident.dto.IncidentResponse;
import com.deloitte.erip.security.CustomUserDetailsService;
import com.deloitte.erip.user.User;
import com.deloitte.erip.vulnerability.Vulnerability;
import com.deloitte.erip.vulnerability.VulnerabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private static final List<IncidentStatus> CLOSED_STATUSES = List.of(IncidentStatus.RESOLVED, IncidentStatus.CLOSED);

    private final IncidentRepository incidentRepository;
    private final AssetRepository assetRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final SecurityEventRepository securityEventRepository;
    private final CustomUserDetailsService userDetailsService;
    private final AuditService auditService;

    @Transactional
    public IncidentResponse create(IncidentRequest request) {
        Asset asset = request.relatedAssetId() != null ? assetRepository.findById(request.relatedAssetId())
                .orElseThrow(() -> ResourceNotFoundException.of("Asset", request.relatedAssetId())) : null;
        Vulnerability vulnerability = request.relatedVulnerabilityId() != null ? vulnerabilityRepository.findById(request.relatedVulnerabilityId())
                .orElseThrow(() -> ResourceNotFoundException.of("Vulnerability", request.relatedVulnerabilityId())) : null;
        SecurityEvent event = request.relatedEventId() != null ? securityEventRepository.findById(request.relatedEventId())
                .orElseThrow(() -> ResourceNotFoundException.of("SecurityEvent", request.relatedEventId())) : null;

        User currentUser = currentUser();

        Incident incident = Incident.builder()
                .incidentNumber(nextIncidentNumber())
                .title(request.title())
                .description(request.description())
                .severity(request.severity())
                .status(IncidentStatus.OPEN)
                .relatedAsset(asset)
                .relatedVulnerability(vulnerability)
                .relatedEvent(event)
                .assignedAnalyst(currentUser)
                .openedAt(Instant.now())
                .build();

        Incident saved = incidentRepository.save(incident);
        auditService.record("INCIDENT_OPENED", "INCIDENT", saved.getId().toString(),
                Map.of("severity", saved.getSeverity().name(), "incidentNumber", saved.getIncidentNumber()));
        return toResponse(saved);
    }

    public IncidentResponse get(UUID id) {
        return toResponse(findOrThrow(id));
    }

    public Page<IncidentResponse> search(IncidentStatus status, IncidentSeverity severity, UUID assetId, Pageable pageable) {
        return incidentRepository.search(status, severity, assetId, pageable).map(this::toResponse);
    }

    @Transactional
    public IncidentResponse updateStatus(UUID id, IncidentStatus status, String note) {
        Incident incident = findOrThrow(id);
        incident.setStatus(status);
        Instant now = Instant.now();
        switch (status) {
            case CONTAINED -> incident.setContainedAt(now);
            case RESOLVED -> {
                incident.setResolvedAt(now);
                incident.setResolutionSummary(note);
            }
            case CLOSED -> incident.setClosedAt(now);
            default -> { /* no timestamp transition for OPEN/INVESTIGATING */ }
        }
        if (status == IncidentStatus.INVESTIGATING && note != null) {
            incident.setRootCause(note);
        }
        Incident saved = incidentRepository.save(incident);
        auditService.record("INCIDENT_STATUS_CHANGED", "INCIDENT", id.toString(), Map.of("status", status.name()));
        return toResponse(saved);
    }

    public long countOpen() {
        return incidentRepository.countByStatusNotIn(CLOSED_STATUSES);
    }

    private String nextIncidentNumber() {
        long sequence = incidentRepository.count() + 1;
        return "INC-%06d".formatted(sequence);
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetails userDetails)) {
            return null;
        }
        return (User) userDetailsService.loadUserByUsername(userDetails.getUsername());
    }

    private Incident findOrThrow(UUID id) {
        return incidentRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Incident", id));
    }

    private IncidentResponse toResponse(Incident i) {
        return new IncidentResponse(
                i.getId(),
                i.getIncidentNumber(),
                i.getTitle(),
                i.getDescription(),
                i.getSeverity(),
                i.getStatus(),
                i.getRelatedAsset() != null ? i.getRelatedAsset().getId() : null,
                i.getRelatedAsset() != null ? i.getRelatedAsset().getName() : null,
                i.getRelatedVulnerability() != null ? i.getRelatedVulnerability().getId() : null,
                i.getAssignedAnalyst() != null ? i.getAssignedAnalyst().getFullName() : null,
                i.getRootCause(),
                i.getResolutionSummary(),
                i.getOpenedAt(),
                i.getContainedAt(),
                i.getResolvedAt(),
                i.getClosedAt());
    }
}
