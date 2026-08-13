package com.example.simplescheduleapp.notification.infrastructure;

import com.example.simplescheduleapp.common.kafka.consumer.KafkaConsumerConfig;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.LectureEventType;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.notification.application.strategy.NotificationCommand;
import com.example.simplescheduleapp.notification.application.strategy.NotificationEventType;
import com.example.simplescheduleapp.notification.application.strategy.NotificationStrategy;
import com.example.simplescheduleapp.notification.application.strategy.NotificationStrategyFactory;
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
            NotificationStrategy strategy = notificationStrategyFactory.getStrategy(toEventType(message.type()));
            strategy.handle(toCommand(message));

            ack.acknowledge();
            log.info("Successfully processed notification event. type: {}", message.type());
        } catch (Exception e) {
            log.error("Failed to process notification event.", e);
            throw e;
        }
    }

    // 전송 계층 어휘 → 유스케이스 어휘. default 없는 switch 식이라 common에 상수가 추가되면
    // 런타임 NOTIFICATION_TYPE_NOT_FOUND가 아니라 이 어댑터의 컴파일 오류로 먼저 드러난다.
    private static NotificationEventType toEventType(LectureEventType type) {
        return switch (type) {
            case ENROLLMENT_REQUESTED -> NotificationEventType.ENROLLMENT_REQUESTED;
            case ENROLLMENT_ACCEPTED -> NotificationEventType.ENROLLMENT_ACCEPTED;
            case ENROLLMENT_REJECTED -> NotificationEventType.ENROLLMENT_REJECTED;
            case ENROLLMENT_CANCELED -> NotificationEventType.ENROLLMENT_CANCELED;
            case LECTURE_UPDATED -> NotificationEventType.LECTURE_UPDATED;
        };
    }

    // Kafka DTO → 커맨드. 변환은 인바운드 어댑터의 책임이다 (ADR-0002).
    private static NotificationCommand toCommand(KafkaLectureEventMessage message) {
        return new NotificationCommand(
                message.lectureId(),
                message.studentId(),
                message.tutorId(),
                message.lectureTitle(),
                message.details()
        );
    }
}
