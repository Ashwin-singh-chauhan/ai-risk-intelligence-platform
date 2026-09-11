package com.deloitte.erip.event.dto;

import com.deloitte.erip.event.EventSeverity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SecurityEventResponse(
        UUID id,
        UUID assetId,
        String assetName,
        String eventType,
        EventSeverity severity,
        String sourceIp,
        String destinationIp,
        String protocol,
        String description,
        Instant eventTime,
        BigDecimal anomalyScore,
        boolean anomaly
) {
}
