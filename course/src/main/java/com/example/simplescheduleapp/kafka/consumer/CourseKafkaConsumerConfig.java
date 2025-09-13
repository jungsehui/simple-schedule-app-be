package com.example.simplescheduleapp.kafka.consumer;

import com.example.simplescheduleapp.common.kafka.consumer.KafkaConsumerProperty;
import com.example.simplescheduleapp.kafka.topic.EnrollSpecialLectureEnrollmentTopicMessage;
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

import static org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class CourseKafkaConsumerConfig {

    public static final String ENROLL_SPECIAL_LECTURE_ENROLLMENT_SUCCESS_CONTAINER_FACTORY = "ENROLL_SPECIAL_LECTURE_ENROLLMENT_SUCCESS_CONTAINER_FACTORY";

    private final KafkaConsumerProperty property;

    @Bean(ENROLL_SPECIAL_LECTURE_ENROLLMENT_SUCCESS_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<String, EnrollSpecialLectureEnrollmentTopicMessage> requestLectureEnrollmentContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EnrollSpecialLectureEnrollmentTopicMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();

        // 수동 커밋
        factory.getContainerProperties().setAckMode(MANUAL_IMMEDIATE);

        factory.setConsumerFactory(enrollSpecialLectureEnrollmentTopicMessageConsumerFactory());

        // 파티션 수와 동일하게 맞춰야 성능이 좋음. 파티션에는 최대 1개의 스레드만 할당됨.
        factory.setConcurrency(1);

        // 예외처리 핸들리
        // 1초 간격 2번 재시도
        FixedBackOff fixedBackOff = new FixedBackOff(1000, 2);
        DefaultErrorHandler defaultErrorHandler = new DefaultErrorHandler((consumerRecord, e) -> {
            log.error("Unexpected exception while consuming topic. offset: {}", consumerRecord.offset());
        }, fixedBackOff);
        factory.setCommonErrorHandler(defaultErrorHandler);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, EnrollSpecialLectureEnrollmentTopicMessage> enrollSpecialLectureEnrollmentTopicMessageConsumerFactory() {
        Map<String, Object> configs = getCourseConfig();
        return new DefaultKafkaConsumerFactory<>(
                configs,
                new StringDeserializer(),
                new JsonDeserializer<>(EnrollSpecialLectureEnrollmentTopicMessage.class, false)
        );
    }

    private Map<String, Object> getCourseConfig() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, property.bootstrapServers());

        // 프로퍼티 파일에 정의된 고정 group.id를 사용
        configs.put(ConsumerConfig.GROUP_ID_CONFIG, property.groupId());

        // 수동 커밋
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        // 처음 연결하는 그룹일 경우, 가장 처음 offset부터 메시지를 가져 옴
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return configs;
    }
}
