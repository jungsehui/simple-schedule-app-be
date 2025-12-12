package com.example.simplescheduleapp.common.kafka.consumer;

import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
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
    public static final String LECTURE_EVENT_CONTAINER_FACTORY = "LECTURE_EVENT_CONTAINER_FACTORY";

    private final DeadLetterRecorder deadLetterRecorder;
    private final KafkaIdempotencyFilter kafkaIdempotencyFilter;
    private final KafkaConsumerProperty property;

    @Bean(LECTURE_EVENT_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<String, KafkaLectureEventMessage> lectureEventContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, KafkaLectureEventMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(lectureEventConsumerFactory());
        factory.getContainerProperties().setAckMode(MANUAL_IMMEDIATE);
        factory.setConcurrency(1);

        // 멱등성 필터 적용 (중복 메시지 걸러내기)
        factory.setRecordFilterStrategy(kafkaIdempotencyFilter);
        factory.setAckDiscarded(true); // 필터링된 메시지도 Ack 처리 (커밋)

        // 에러 핸들러 (1초 간격 2회 재시도 후 DLQ 저장)
//        FixedBackOff fixedBackOff = new FixedBackOff(1000L, 2L);
        FixedBackOff fixedBackOff = new FixedBackOff(0, 0);
        DefaultErrorHandler defaultErrorHandler = new DefaultErrorHandler(deadLetterRecorder, fixedBackOff);
        factory.setCommonErrorHandler(defaultErrorHandler);

        return factory;
    }

    @Bean
    public ConsumerFactory<String, KafkaLectureEventMessage> lectureEventConsumerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, property.bootstrapServers());
        configs.put(ConsumerConfig.GROUP_ID_CONFIG, property.groupId());
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // KafkaLectureEventMessage 타입으로 역직렬화
        JsonDeserializer<KafkaLectureEventMessage> deserializer = new JsonDeserializer<>(KafkaLectureEventMessage.class, false);
        deserializer.addTrustedPackages("*"); // 모든 패키지 신뢰

        return new DefaultKafkaConsumerFactory<>(configs, new StringDeserializer(), deserializer);
    }
}
