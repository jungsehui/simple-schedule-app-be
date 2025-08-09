package com.example.simplescheduleapp.kafka.consumer;

import com.example.simplescheduleapp.common.kafka.KafkaDomainEventMessage;
import com.example.simplescheduleapp.common.kafka.consumer.KafkaConsumerConfig;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.kafka.event.NotificationMessageEvent;
import com.example.simplescheduleapp.kafka.topic.AcceptLectureEnrollmentTopicMessage;
import com.example.simplescheduleapp.kafka.topic.CancelLectureEnrollmentTopicMessage;
import com.example.simplescheduleapp.kafka.topic.RejectLectureEnrollmentTopicMessage;
import com.example.simplescheduleapp.kafka.topic.RequestLectureEnrollmentTopicMessage;
import com.example.simplescheduleapp.lecture.presentation.response.EnrolledStudentInfosResponse;
import com.example.simplescheduleapp.notification.application.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaConsumer {

    private final RestTemplate restTemplate;
    private final NotificationService notificationService;

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

        // course 모듈의 API 호출로 수강생 ID 목록을 가져 옴
        // 실제 구현에서는 Service Discovery 또는 설정 파일 등을 통해 course 서비스의 주소를 가져 와야 함
        String courseApiUrl = "http://localhost:8080/internal/lectures/" + message.targetDomainId() + "/student-ids";

        ParameterizedTypeReference<EnrolledStudentInfosResponse> responseType = new ParameterizedTypeReference<>() {};

        ResponseEntity<EnrolledStudentInfosResponse> responseEntity = restTemplate.exchange(
                courseApiUrl,
                HttpMethod.GET,
                null, // 요청 본문이 없으므로 null
                responseType
        );

        String title = responseEntity.getBody().lectureTitle();
        String memo = responseEntity.getBody().lectureMemo();
        List<Long> studentIds = responseEntity.getBody().studentIds();
        if (studentIds == null || studentIds.isEmpty()) {
            return;
        }

        // 각 학생에게 개별적으로 알림 처리
        for (Long studentId : studentIds) {
            NotificationMessageEvent event = new NotificationMessageEvent(
                    message.targetDomainId(),
                    studentId,
                    title,
                    "강의 내용이 수정되었습니다. 수정 내용: {%s}".formatted(memo)
            );
            notificationService.sendPushNotification(event);
        }
        ack.acknowledge();
        log.info("Successfully consume lecture updated event topic. id :{}, uuid: {}, offset: {}",
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
