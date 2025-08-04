package com.example.simplescheduleapp.kafka.consumer;

import com.example.simplescheduleapp.common.kafka.consumer.KafkaConsumerProperty;
import com.example.simplescheduleapp.kafka.topic.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.UUID;

import static org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class NotificationKafkaConsumerConfig {

    public static final String REQUEST_LECTURE_ENROLLMENT_CONTAINER_FACTORY = "REQUEST_LECTURE_ENROLLMENT_CONTAINER_FACTORY";
    public static final String ACCEPT_LECTURE_ENROLLMENT_CONTAINER_FACTORY = "ACCEPT_LECTURE_ENROLLMENT_CONTAINER_FACTORY";
    public static final String REJECT_LECTURE_ENROLLMENT_CONTAINER_FACTORY = "REJECT_LECTURE_ENROLLMENT_CONTAINER_FACTORY";
    public static final String CANCEL_LECTURE_ENROLLMENT_CONTAINER_FACTORY = "CANCEL_LECTURE_ENROLLMENT_CONTAINER_FACTORY";

    private final KafkaConsumerProperty property;

    @Bean
    public ConsumerFactory<String, RequestLectureEnrollmentTopicMessage> requestLectureEnrollmentTopicMessageConsumerFactory() {
        Map<String, Object> configs = getNotificationConfig();
        return new DefaultKafkaConsumerFactory<>(
                configs,
                new StringDeserializer(),
                new JsonDeserializer<>(RequestLectureEnrollmentTopicMessage.class, false)
        );
    }

    @Bean(REQUEST_LECTURE_ENROLLMENT_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<String, RequestLectureEnrollmentTopicMessage> requestLectureEnrollmentContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, RequestLectureEnrollmentTopicMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        // 수동 커밋
        factory.getContainerProperties().setAckMode(MANUAL_IMMEDIATE);

        factory.setConsumerFactory(requestLectureEnrollmentTopicMessageConsumerFactory());

        // 파티션 수와 동일하게 맞춰야 성능이 좋음. 파티션에는 최대 1개의 스레드만 할당됨.
        factory.setConcurrency(1);

        // 예외처리 핸들리
        // 1초 간격 2번 재시도
        FixedBackOff fixedBackOff = new FixedBackOff(1000, 2);
        DefaultErrorHandler defaultErrorHandler = new DefaultErrorHandler((consumerRecord, e) -> {
            log.error("Unexpected exception while consume chat domain topic. offset: {}", consumerRecord.offset());
        }, fixedBackOff);
        factory.setCommonErrorHandler(defaultErrorHandler);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, AcceptLectureEnrollmentTopicMessage> acceptLectureEnrollmentTopicMessageConsumerFactory() {
        Map<String, Object> configs = getNotificationConfig();
        return new DefaultKafkaConsumerFactory<>(
                configs,
                new StringDeserializer(),
                new JsonDeserializer<>(AcceptLectureEnrollmentTopicMessage.class, false)
        );
    }

    @Bean(ACCEPT_LECTURE_ENROLLMENT_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<String, AcceptLectureEnrollmentTopicMessage> acceptLectureEnrollmentContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, AcceptLectureEnrollmentTopicMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        // 수동 커밋
        factory.getContainerProperties().setAckMode(MANUAL_IMMEDIATE);

        factory.setConsumerFactory(acceptLectureEnrollmentTopicMessageConsumerFactory());

        // 파티션 수와 동일하게 맞춰야 성능이 좋음. 파티션에는 최대 1개의 스레드만 할당됨.
        factory.setConcurrency(1);

        // 예외처리 핸들리
        // 1초 간격 2번 재시도
        FixedBackOff fixedBackOff = new FixedBackOff(1000, 2);
        DefaultErrorHandler defaultErrorHandler = new DefaultErrorHandler((consumerRecord, e) -> {
            log.error("Unexpected exception while consume chat domain topic. offset: {}", consumerRecord.offset());
        }, fixedBackOff);
        factory.setCommonErrorHandler(defaultErrorHandler);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, RejectLectureEnrollmentTopicMessage> rejectLectureEnrollmentTopicMessageConsumerFactory() {
        Map<String, Object> configs = getNotificationConfig();
        return new DefaultKafkaConsumerFactory<>(
                configs,
                new StringDeserializer(),
                new JsonDeserializer<>(RejectLectureEnrollmentTopicMessage.class, false)
        );
    }

    @Bean(REJECT_LECTURE_ENROLLMENT_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<String, RejectLectureEnrollmentTopicMessage> rejectLectureEnrollmentContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, RejectLectureEnrollmentTopicMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        // 수동 커밋
        factory.getContainerProperties().setAckMode(MANUAL_IMMEDIATE);

        factory.setConsumerFactory(rejectLectureEnrollmentTopicMessageConsumerFactory());

        // 파티션 수와 동일하게 맞춰야 성능이 좋음. 파티션에는 최대 1개의 스레드만 할당됨.
        factory.setConcurrency(1);

        // 예외처리 핸들리
        // 1초 간격 2번 재시도
        FixedBackOff fixedBackOff = new FixedBackOff(1000, 2);
        DefaultErrorHandler defaultErrorHandler = new DefaultErrorHandler((consumerRecord, e) -> {
            log.error("Unexpected exception while consume chat domain topic. offset: {}", consumerRecord.offset());
        }, fixedBackOff);
        factory.setCommonErrorHandler(defaultErrorHandler);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, CancelLectureEnrollmentTopicMessage> cancelLectureEnrollmentTopicMessageConsumerFactory() {
        Map<String, Object> configs = getNotificationConfig();
        return new DefaultKafkaConsumerFactory<>(
                configs,
                new StringDeserializer(),
                new JsonDeserializer<>(CancelLectureEnrollmentTopicMessage.class, false)
        );
    }

    @Bean(CANCEL_LECTURE_ENROLLMENT_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<String, CancelLectureEnrollmentTopicMessage> cancelLectureEnrollmentContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CancelLectureEnrollmentTopicMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        // 수동 커밋
        factory.getContainerProperties().setAckMode(MANUAL_IMMEDIATE);

        factory.setConsumerFactory(cancelLectureEnrollmentTopicMessageConsumerFactory());

        // 파티션 수와 동일하게 맞춰야 성능이 좋음. 파티션에는 최대 1개의 스레드만 할당됨.
        factory.setConcurrency(1);

        // 예외처리 핸들리
        // 1초 간격 2번 재시도
        FixedBackOff fixedBackOff = new FixedBackOff(1000, 2);
        DefaultErrorHandler defaultErrorHandler = new DefaultErrorHandler((consumerRecord, e) -> {
            log.error("Unexpected exception while consume chat domain topic. offset: {}", consumerRecord.offset());
        }, fixedBackOff);
        factory.setCommonErrorHandler(defaultErrorHandler);
        return factory;
    }

    private Map<String, Object> getNotificationConfig() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, property.bootstrapServers());

        String uuid = UUID.randomUUID().toString();
        // scale out 된 여러 chat server 에서 동일한 topic 의 메세지를 모두 받아오기 위해 consumer 그룹을 다르게 지정한다.
        configs.put(ConsumerConfig.GROUP_ID_CONFIG, property.groupId() + uuid);

        // 수동 커밋
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        // earliest 로 설정하면 애플리케이션이 뜰 때마다 consumer 그룹이 새로 만들어져 지금까지 진행된 모든 대화 메세지 topic 을 읽어온다.
        // 이를 방지하기 위해 latest 로 설정한다.
        // 메세지 유실은 걱정하지 않아도 되는게, 메세지를 보내기 전 어차피 db 에 메세지를 저장한다. 따라서 채팅방 입장 시 읽어오게 된다.
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return configs;
    }
}
