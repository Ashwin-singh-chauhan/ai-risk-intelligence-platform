package com.deloitte.erip.event;

import com.deloitte.erip.anomaly.AnomalyClient;
import com.deloitte.erip.anomaly.dto.AnomalyScoreRequest;
import com.deloitte.erip.anomaly.dto.AnomalyScoreResponse;
import com.deloitte.erip.asset.Asset;
import com.deloitte.erip.asset.AssetRepository;
import com.deloitte.erip.event.dto.SecurityEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityEventConsumer {

    private final SecurityEventRepository securityEventRepository;
    private final AssetRepository assetRepository;
    private final AnomalyClient anomalyClient;
    private final SecurityEventProducer producer;

    @KafkaListener(topics = "${erip.kafka.topics.security-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(SecurityEventMessage message) {
        try {
            Asset asset = resolveAsset(message);
            SecurityEvent event = SecurityEvent.builder()
                    .asset(asset)
                    .eventType(message.eventType())
                    .severity(message.severity() != null ? message.severity() : EventSeverity.INFO)
                    .sourceIp(message.sourceIp())
                    .destinationIp(message.destinationIp())
                    .sourcePort(message.sourcePort())
                    .destinationPort(message.destinationPort())
                    .protocol(message.protocol())
                    .description(message.description())
                    .rawPayload(message.rawPayload())
                    .eventTime(message.eventTime() != null ? message.eventTime() : Instant.now())
                    .ingestedAt(Instant.now())
                    .anomaly(false)
                    .build();

            applyAnomalyScore(event, asset);

            securityEventRepository.save(event);
        } catch (Exception ex) {
            log.error("Failed to process security event message, routing to DLQ", ex);
            producer.publishToDlq(message, ex.getMessage());
        }
    }

    private Asset resolveAsset(SecurityEventMessage message) {
        if (message.assetId() != null) {
            return assetRepository.findById(message.assetId()).orElse(null);
        }
        if (message.assetHint() != null) {
            return assetRepository.findFirstByIpAddressOrHostname(message.assetHint(), message.assetHint()).orElse(null);
        }
        if (message.sourceIp() != null) {
            return assetRepository.findFirstByIpAddressOrHostname(message.sourceIp(), message.sourceIp()).orElse(null);
        }
        return null;
    }

    private void applyAnomalyScore(SecurityEvent event, Asset asset) {
        ZonedDateTime zdt = event.getEventTime().atZone(ZoneOffset.UTC);
        AnomalyScoreRequest request = new AnomalyScoreRequest(
                event.getEventType(),
                event.getSeverity().name(),
                event.getSourcePort(),
                event.getDestinationPort(),
                event.getProtocol(),
                zdt.getHour(),
                zdt.getDayOfWeek().getValue(),
                asset != null ? asset.getCriticality().weight() : 1,
                asset != null ? asset.getExposure().weight() : 1);

        Optional<AnomalyScoreResponse> scored = anomalyClient.score(request);
        scored.ifPresent(response -> {
            event.setAnomalyScore(BigDecimal.valueOf(response.anomalyScore()));
            event.setAnomaly(response.isAnomaly());
            event.setScoredAt(Instant.now());
        });
    }
}
