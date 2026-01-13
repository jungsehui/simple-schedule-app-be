package com.example.simplescheduleapp.kafka.consumer;

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
        log.info("Consume notification event. type: {}, targetId: {}, offset: {}",
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

//        // 2. ForkJoinPool parallelStream 사용
//        studentIds.stream().parallel().forEach(studentId -> {
//            try {
//                NotificationRequest event = new NotificationRequest(
//                        message.targetDomainId(),
//                        studentId,
//                        title,
//                        "강의 내용이 수정되었습니다. 수정 내용: {%s}".formatted(memo)
//                );
//                notificationService.sendPushNotification(event);
//                log.info("Sent notification message. to student ID: {}", studentId);
//            } catch (Exception e) {
//                // 개별 알림 실패 시 로그 기록. 필요 시 DLQ 전송 등의 로직 추가 가능
//                log.error("Failed to send notification to student ID: {}. Error: {}", studentId, e.getMessage());
//            }
//        });

//        // 1. 단순 반복문 각 학생에게 개별적으로 알림 처리
//        for (Long studentId : studentIds) {
//            NotificationRequest event = new NotificationRequest(
//                    message.targetDomainId(),
//                    studentId,
//                    title,
//                    "강의 내용이 수정되었습니다. 수정 내용: {%s}".formatted(memo)
//            );
//            notificationService.sendPushNotification(event);
//            log.info("send notification message. to student ID: {}", studentId);
//        }
