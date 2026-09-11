package com.deloitte.erip.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${erip.kafka.topics.security-events}")
    private String securityEventsTopic;

    @Value("${erip.kafka.topics.security-events-dlq}")
    private String securityEventsDlqTopic;

    @Bean
    public NewTopic securityEventsTopic() {
        return TopicBuilder.name(securityEventsTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic securityEventsDlqTopic() {
        return TopicBuilder.name(securityEventsDlqTopic).partitions(1).replicas(1).build();
    }
}
