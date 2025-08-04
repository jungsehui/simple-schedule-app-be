package com.example.simplescheduleapp.kafka.consumer;

import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.kafka.topic.AcceptLectureEnrollmentTopicMessage;
import com.example.simplescheduleapp.kafka.topic.CancelLectureEnrollmentTopicMessage;
import com.example.simplescheduleapp.kafka.topic.RejectLectureEnrollmentTopicMessage;
import com.example.simplescheduleapp.kafka.topic.RequestLectureEnrollmentTopicMessage;
import com.example.simplescheduleapp.notification.application.NotificationService;
import com.example.simplescheduleapp.kafka.event.NotificationMessageEvent;
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
public class KafkaConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = KafkaTopics.REQUEST_LECTURE_ENROLLMENT_TOPIC,
            containerFactory = NotificationKafkaConsumerConfig.REQUEST_LECTURE_ENROLLMENT_CONTAINER_FACTORY
    )
    public void consumeLectureEnrollmentRequest(
            RequestLectureEnrollmentTopicMessage message,
            Acknowledgment ack,
            @Header(KafkaHeaders.OFFSET) int offset
    ) {
        log.info("Try to consume request lecture enrollment topic. id: {}, offset: {}", message.senderId(), offset);
        NotificationMessageEvent event = new NotificationMessageEvent(
                message.senderId(),
                message.targetId(),
                message.lectureTitle(),
                "수강신청 알림을 전송합니다."
        );
        notificationService.sendPushNotification(event);
        ack.acknowledge();
        log.info("Successfully consume send chat message topic. id :{}, offset: {}", message.senderId(), offset);
    }

    @KafkaListener(
            topics = KafkaTopics.ACCEPT_LECTURE_ENROLLMENT_TOPIC,
            containerFactory = NotificationKafkaConsumerConfig.ACCEPT_LECTURE_ENROLLMENT_CONTAINER_FACTORY
    )
    public void consumeLectureEnrollmentAcceptance(
            AcceptLectureEnrollmentTopicMessage message,
            Acknowledgment ack,
            @Header(KafkaHeaders.OFFSET) int offset
    ) {
        log.info("Try to consume accept lecture enrollment topic. id: {}, offset: {}", message.senderId(), offset);
        NotificationMessageEvent event = new NotificationMessageEvent(
                message.senderId(),
                message.targetId(),
                message.lectureTitle(),
                "수강신청 수락 알림을 전송합니다."
        );
        notificationService.sendPushNotification(event);
        ack.acknowledge();
        log.info("Successfully consume accept lecture enrollment topic. id: {}, offset: {}", message.senderId(), offset);
    }

    @KafkaListener(
            topics = KafkaTopics.REJECT_LECTURE_ENROLLMENT_TOPIC,
            containerFactory = NotificationKafkaConsumerConfig.REJECT_LECTURE_ENROLLMENT_CONTAINER_FACTORY
    )
    public void consumeLectureEnrollmentRejection(
            RejectLectureEnrollmentTopicMessage message,
            Acknowledgment ack,
            @Header(KafkaHeaders.OFFSET) int offset
    ) {
        log.info("Try to consume reject lecture enrollment topic. id: {}, offset: {}", message.senderId(), offset);
        NotificationMessageEvent event = new NotificationMessageEvent(
                message.senderId(),
                message.targetId(),
                message.lectureTitle(),
                "수강신청 거절 알림을 전송합니다."
        );
        notificationService.sendPushNotification(event);
        ack.acknowledge();
        log.info("Successfully consume reject lecture enrollment topic. id: {}, offset: {}", message.senderId(), offset);
    }

    @KafkaListener(
            topics = KafkaTopics.CANCEL_LECTURE_ENROLLMENT_TOPIC,
            containerFactory = NotificationKafkaConsumerConfig.CANCEL_LECTURE_ENROLLMENT_CONTAINER_FACTORY
    )
    public void consumeLectureEnrollmentCancel(
            CancelLectureEnrollmentTopicMessage message,
            Acknowledgment ack,
            @Header(KafkaHeaders.OFFSET) int offset
    ) {
        log.info("Try to consume cancel lecture enrollment topic. id: {}, offset: {}", message.senderId(), offset);
        NotificationMessageEvent event = new NotificationMessageEvent(
                message.senderId(),
                message.targetId(),
                message.lectureTitle(),
                "수강신청 취소 알림을 전송합니다."
        );
        notificationService.sendPushNotification(event);
        ack.acknowledge();
        log.info("Successfully consume cancel lecture enrollment topic. id: {}, offset: {}", message.senderId(), offset);
    }
}
