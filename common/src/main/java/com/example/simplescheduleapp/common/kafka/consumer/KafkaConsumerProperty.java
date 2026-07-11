package com.example.simplescheduleapp.common.kafka.consumer;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.kafka.consumer")
public record KafkaConsumerProperty(
        String bootstrapServers,
        String groupId
) {
}
