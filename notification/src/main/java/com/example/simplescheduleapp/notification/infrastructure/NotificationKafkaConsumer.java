package com.example.simplescheduleapp.notification.infrastructure;

import com.example.simplescheduleapp.common.kafka.consumer.KafkaConsumerConfig;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.notification.strategy.NotificationStrategy;
import com.example.simplescheduleapp.notification.strategy.NotificationStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationKafkaConsumer {

    // 전략 구현체 주입
    private final NotificationStrategyFactory notificationStrategyFactory;

    @KafkaListener(
            topics = KafkaTopics.COURSE_EVENT_TOPIC, // 통합 토픽 하나만 리스닝, 필요 시 토픽 분리 아니면 파티션 분리
            containerFactory = KafkaConsumerConfig.LECTURE_EVENT_CONTAINER_FACTORY
    )
    public void consumeNotificationEvent(
            KafkaLectureEventMessage message,
            Acknowledgment ack,
            @Header(KafkaHeaders.OFFSET) int offset
    ) {
        log.info("Consume notification event. type: {}, lectureId: {}, offset: {}",
                message.type(), message.lectureId(), offset);

        try {
            NotificationStrategy strategy = notificationStrategyFactory.getStrategy(message.type());
            strategy.handle(message);

            ack.acknowledge();
            log.info("Successfully processed notification event. type: {}", message.type());
        } catch (Exception e) {
            log.error("Failed to process notification event.", e);
            throw e;
        }
    }
}
