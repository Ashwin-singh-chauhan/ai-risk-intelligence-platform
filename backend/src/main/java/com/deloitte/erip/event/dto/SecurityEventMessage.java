package com.deloitte.erip.event.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.deloitte.erip.event.EventSeverity;

import java.time.Instant;
import java.util.UUID;

/**
 * Wire format published to / consumed from the {@code security-events} Kafka topic.
 * Represents a normalized event as it would arrive from a SIEM, EDR, firewall or cloud log source.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SecurityEventMessage(
        UUID assetId,
        String assetHint,
        String eventType,
        EventSeverity severity,
        String sourceIp,
        String destinationIp,
        Integer sourcePort,
        Integer destinationPort,
        String protocol,
        String description,
        Instant eventTime,
        JsonNode rawPayload
) {
}
