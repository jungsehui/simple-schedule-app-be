package com.example.simplescheduleapp.kafka.consumer;

import com.example.simplescheduleapp.common.kafka.KafkaDomainEventMessage;
import com.example.simplescheduleapp.common.kafka.consumer.KafkaConsumerConfig;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.kafka.event.NotificationMessageEvent;
import com.example.simplescheduleapp.kafka.topic.AcceptLectureEnrollmentTopicMessage;
import com.example.simplescheduleapp.kafka.topic.CancelLectureEnrollmentTopicMessage;
import com.example.simplescheduleapp.kafka.topic.RejectLectureEnrollmentTopicMessage;
import com.example.simplescheduleapp.kafka.topic.RequestLectureEnrollmentTopicMessage;
import com.example.simplescheduleapp.notification.application.NotificationFacade;
import com.example.simplescheduleapp.notification.application.NotificationService;
import com.example.simplescheduleapp.notification.client.CourseClient;
import com.example.simplescheduleapp.notification.client.response.GetEnrolledStudentInfosResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaConsumer {

    private final NotificationService notificationService;
    private final NotificationFacade notificationFacade;
    private final CourseClient courseClient;

    @KafkaListener(
            topics = KafkaTopics.LECTURE_UPDATED_TOPIC,
            containerFactory = KafkaConsumerConfig.DOMAIN_EVENT_CONTAINER_FACTORY
    )
    public void consumeLectureUpdatedEvent(
            KafkaDomainEventMessage message,
            Acknowledgment ack,
            @Header(KafkaHeaders.OFFSET) int offset
    ) {
        log.info("Try to consume lecture updated event topic. id: {}, uuid: {}, offset: {}",
                message.targetDomainId(), message.uuid(), offset);

        GetEnrolledStudentInfosResponse studentInfos = courseClient.getEnrolledStudentInfosByLectureId(message.targetDomainId());
        List<Long> studentIds = studentInfos.studentIds();
        if (studentIds == null || studentIds.isEmpty()) {
            ack.acknowledge();
            return;
        }

        notificationFacade.sendNotificationsAsync(
                message.targetDomainId(),
                studentIds,
                studentInfos.lectureTitle(),
                "강의 내용이 수정되었습니다. 수정 내용: {%s}".formatted(studentInfos.lectureMemo())
        );

        ack.acknowledge();
        log.info("Successfully consume lecture updated event topic. id: {}, uuid: {}, offset: {}",
                message.targetDomainId(), message.uuid(), offset);
    }

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
        notificationFacade.sendNotification(
                message.senderId(),
                message.targetId(),
                message.lectureTitle(),
                "새로운 수강 신청 요청이 도착했습니다."
        );
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
        notificationFacade.sendNotification(
                message.senderId(),
                message.targetId(),
                message.lectureTitle(),
                "수강 신청이 수락되었습니다 !"
        );
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
        notificationFacade.sendNotification(
                message.senderId(),
                message.targetId(),
                message.lectureTitle(),
                "수강 신청이 거절되었습니다 .."
        );
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
        notificationFacade.sendNotification(
                message.senderId(),
                message.targetId(),
                message.lectureTitle(),
                "수강 신청을 취소하였습니다 .."
        );
        ack.acknowledge();
        log.info("Successfully consume cancel lecture enrollment topic. id: {}, offset: {}", message.senderId(), offset);
    }
}

//        // 2. ForkJoinPool parallelStream 사용
//        studentIds.stream().parallel().forEach(studentId -> {
//            try {
//                NotificationMessageEvent event = new NotificationMessageEvent(
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
//            NotificationMessageEvent event = new NotificationMessageEvent(
//                    message.targetDomainId(),
//                    studentId,
//                    title,
//                    "강의 내용이 수정되었습니다. 수정 내용: {%s}".formatted(memo)
//            );
//            notificationService.sendPushNotification(event);
//            log.info("send notification message. to student ID: {}", studentId);
//        }
