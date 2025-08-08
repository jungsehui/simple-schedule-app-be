package com.example.simplescheduleapp.common.kafka.consumer;

import com.example.simplescheduleapp.common.kafka.KafkaDomainEventMessage;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Configuration
public class KafkaConsumerConfig {

    public static final String DOMAIN_EVENT_CONTAINER_FACTORY = "DOMAIN_EVENT_CONTAINER_FACTORY";

    private final KafkaConsumerProperty property;

    @Bean(DOMAIN_EVENT_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<String, KafkaDomainEventMessage> domainEventContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, KafkaDomainEventMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(defaultDomainEventConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, KafkaDomainEventMessage> defaultDomainEventConsumerFactory() {
        Map<String, Object> configs = getDefaultConfigs();
        return new DefaultKafkaConsumerFactory<>(
                configs,
                new StringDeserializer(),
                new JsonDeserializer<>(KafkaDomainEventMessage.class, false));
    }

    private Map<String, Object> getDefaultConfigs() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, property.bootstrapServers());
        configs.put(ConsumerConfig.GROUP_ID_CONFIG, property.groupId());

        // 수동 커밋
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        // 메세지 유실을 방지하기 위해 earliest 로 설정
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return configs;
    }
}
