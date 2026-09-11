package com.deloitte.erip.event;

import com.deloitte.erip.event.dto.SecurityEventResponse;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public SecurityEventResponse toResponse(SecurityEvent e) {
        return new SecurityEventResponse(
                e.getId(),
                e.getAsset() != null ? e.getAsset().getId() : null,
                e.getAsset() != null ? e.getAsset().getName() : null,
                e.getEventType(),
                e.getSeverity(),
                e.getSourceIp(),
                e.getDestinationIp(),
                e.getProtocol(),
                e.getDescription(),
                e.getEventTime(),
                e.getAnomalyScore(),
                e.isAnomaly());
    }
}
