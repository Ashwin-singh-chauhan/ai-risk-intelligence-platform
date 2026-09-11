package com.deloitte.erip.event;

import com.deloitte.erip.event.dto.SecurityEventMessage;
import com.deloitte.erip.event.dto.SecurityEventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final SecurityEventRepository securityEventRepository;
    private final SecurityEventProducer producer;
    private final EventMapper eventMapper;

    public void ingest(SecurityEventMessage message) {
        producer.publish(message);
    }

    public Page<SecurityEventResponse> search(UUID assetId, EventSeverity severity, boolean anomalyOnly,
                                               String eventType, Pageable pageable) {
        return securityEventRepository.search(assetId, severity, anomalyOnly, eventType, pageable)
                .map(eventMapper::toResponse);
    }

    public Page<SecurityEventResponse> listAnomalies(Pageable pageable) {
        return securityEventRepository.findAllByAnomalyTrueOrderByEventTimeDesc(pageable).map(eventMapper::toResponse);
    }

    public EventStats statsForWindow(int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        long total = securityEventRepository.countByEventTimeAfter(since);
        long anomalies = securityEventRepository.countByAnomalyTrueAndEventTimeAfter(since);
        return new EventStats(total, anomalies, hours);
    }

    public record EventStats(long totalEvents, long anomalyEvents, int windowHours) {
    }
}
