package com.deloitte.erip.event;

import com.deloitte.erip.common.dto.PageResponse;
import com.deloitte.erip.event.dto.SecurityEventMessage;
import com.deloitte.erip.event.dto.SecurityEventResponse;
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
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Security Events", description = "Kafka-ingested security events with ML anomaly scores")
public class EventController {

    private final EventService eventService;

    @PostMapping("/ingest")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_ANALYST')")
    @Operation(summary = "Publish a normalized security event onto the Kafka ingestion pipeline")
    public ResponseEntity<Void> ingest(@Valid @RequestBody SecurityEventMessage message) {
        eventService.ingest(message);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping
    @Operation(summary = "Search and paginate ingested security events")
    public ResponseEntity<PageResponse<SecurityEventResponse>> search(
            @RequestParam(required = false) UUID assetId,
            @RequestParam(required = false) EventSeverity severity,
            @RequestParam(defaultValue = "false") boolean anomalyOnly,
            @RequestParam(required = false) String eventType,
            Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(eventService.search(assetId, severity, anomalyOnly, eventType, pageable)));
    }

    @GetMapping("/anomalies")
    @Operation(summary = "List ML-flagged anomalous events")
    public ResponseEntity<PageResponse<SecurityEventResponse>> anomalies(Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(eventService.listAnomalies(pageable)));
    }

    @GetMapping("/stats")
    @Operation(summary = "Rolling window event and anomaly counts")
    public ResponseEntity<EventService.EventStats> stats(@RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(eventService.statsForWindow(hours));
    }
}
