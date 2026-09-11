package com.deloitte.erip.event;

import com.deloitte.erip.event.dto.SecurityEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${erip.kafka.topics.security-events}")
    private String securityEventsTopic;

    @Value("${erip.kafka.topics.security-events-dlq}")
    private String dlqTopic;

    public void publish(SecurityEventMessage message) {
        String key = message.assetId() != null ? message.assetId().toString() : UUID.randomUUID().toString();
        kafkaTemplate.send(securityEventsTopic, key, message)
                .exceptionally(ex -> {
                    log.error("Failed to publish security event to {}", securityEventsTopic, ex);
                    return null;
                });
    }

    public void publishToDlq(Object payload, String reason) {
        log.warn("Routing message to DLQ: {}", reason);
        kafkaTemplate.send(dlqTopic, payload);
    }
}
