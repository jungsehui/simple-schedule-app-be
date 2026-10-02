package com.example.simplescheduleapp.common.kafka.consumer;

import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.deadletter.DeadLetterRecorder;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE;

// Boot 4는 KafkaAutoConfiguration을 spring-boot-autoconfigure에서 별도 spring-boot-kafka 모듈로 옮겼다.
// 본 프로젝트는 spring-kafka를 직접 의존(그 Boot 모듈 미포함)하므로 @KafkaListener 자동 활성화가 사라진다.
// 앱이 template·factory·container를 전부 자체 구성하므로, 리스너 컨테이너 팩토리를 소유한 이 설정에
// @EnableKafka를 명시해 리스너 BPP만 보충한다(Boot 3.4는 autoconfigure에 있어 무의식적으로 활성화됨). (ADR-0003 Stage 3)
@EnableKafka
@RequiredArgsConstructor
@Configuration
public class KafkaConsumerConfig {

    public static final String DOMAIN_EVENT_CONTAINER_FACTORY = "DOMAIN_EVENT_CONTAINER_FACTORY";
    public static final String LECTURE_EVENT_CONTAINER_FACTORY = "LECTURE_EVENT_CONTAINER_FACTORY";

    private final DeadLetterRecorder deadLetterRecorder;
    private final KafkaIdempotencyFilter kafkaIdempotencyFilter;
    private final KafkaProcessedRecordInterceptor kafkaProcessedRecordInterceptor;
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
        // 처리 기록은 리스너 성공 후에만 남긴다 (필터는 확인만 한다)
        factory.setRecordInterceptor(kafkaProcessedRecordInterceptor);

        // 에러 핸들러 (1초 간격 2회 재시도 후 Dead Letter 저장)
        FixedBackOff fixedBackOff = new FixedBackOff(1000L, 2L);
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
        deserializer.addTrustedPackages("com.example.simplescheduleapp");

        return new DefaultKafkaConsumerFactory<>(configs, new StringDeserializer(), deserializer);
    }
}
