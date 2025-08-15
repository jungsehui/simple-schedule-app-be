package com.example.simplescheduleapp.common.kafka.consumer;

import com.example.simplescheduleapp.common.kafka.KafkaDomainEventMessage;
import com.example.simplescheduleapp.common.kafka.deadletter.DeadLetterRecorder;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE;

@RequiredArgsConstructor
@Configuration
public class KafkaConsumerConfig {

    public static final String DOMAIN_EVENT_CONTAINER_FACTORY = "DOMAIN_EVENT_CONTAINER_FACTORY";

    private final DeadLetterRecorder deadLetterRecorder;
    private final KafkaIdempotencyFilter kafkaIdempotencyFilter;
    private final KafkaConsumerProperty property;

    @Bean(DOMAIN_EVENT_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<String, KafkaDomainEventMessage> domainEventContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, KafkaDomainEventMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();

        // 수동 커밋
        factory.getContainerProperties().setAckMode(MANUAL_IMMEDIATE);

        factory.setConsumerFactory(defaultDomainEventConsumerFactory());

        // 파티션 수와 동일하게 맞춰야 성능이 좋음, 파티션에는 최대 1개의 쓰레드만 할당됨
        factory.setConcurrency(1);

        // record 필터, interceptor 이전에 실행됨
        // retry 시에도 동일하게 호출됨
        factory.setRecordFilterStrategy(kafkaIdempotencyFilter);
        factory.setAckDiscarded(true);

        FixedBackOff fixedBackOff = new FixedBackOff(0, 0);
        DefaultErrorHandler defaultErrorHandler = new DefaultErrorHandler(deadLetterRecorder, fixedBackOff);
        factory.setCommonErrorHandler(defaultErrorHandler);
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
