package com.example.simplescheduleapp.event.mapper;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.event.exception.DomainEventExceptionCode;
import com.example.simplescheduleapp.common.event.mapper.DomainEventMapper;
import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.LectureEventType;
import com.example.simplescheduleapp.event.*;
import org.springframework.stereotype.Component;

@Component
public class CourseDomainEventMapper implements DomainEventMapper {

    @Override
    public boolean supports(DomainEvent event) {
        return
                event instanceof LectureEnrollmentAcceptedEvent  ||
                event instanceof LectureUpdatedEvent             ||
                event instanceof LectureEnrollmentRequestedEvent ||
                event instanceof LectureEnrollmentCanceledEvent  ||
                event instanceof LectureEnrollmentRejectedEvent
                ;
    }

    @Override
    public KafkaLectureEventMessage mapToMessage(DomainEvent event) {
        return switch (event) {
            case LectureEnrollmentAcceptedEvent e -> KafkaLectureEventMessage.create(
                    e.getUuid(),
                    LectureEventType.ENROLLMENT_ACCEPTED,
                    e.getTargetDomainId(),
                    e.getStudentId(),
                    e.getTutorId(),
                    e.getLectureTitle(),
                    "수강 신청이 수락되었습니다 !"
            );

            case LectureEnrollmentCanceledEvent e -> KafkaLectureEventMessage.create(
                    e.getUuid(),
                    LectureEventType.ENROLLMENT_CANCELED,
                    e.getTargetDomainId(),
                    e.getStudentId(),
                    e.getTutorId(),
                    e.getLectureTitle(),
                    "학생이 수강 신청을 취소하였습니다 .."
            );

            case LectureEnrollmentRejectedEvent e -> KafkaLectureEventMessage.create(
                    e.getUuid(),
                    LectureEventType.ENROLLMENT_REJECTED,
                    e.getTargetDomainId(),
                    e.getStudentId(),
                    e.getTutorId(),
                    e.getLectureTitle(),
                    "수강 신청이 거절되었습니다 .."
            );

            case LectureEnrollmentRequestedEvent e -> KafkaLectureEventMessage.create(
                    e.getUuid(),
                    LectureEventType.ENROLLMENT_REQUESTED,
                    e.getTargetDomainId(),
                    e.getStudentId(),
                    e.getTutorId(),
                    e.getLectureTitle(),
                    "새로운 수강 신청 요청이 도착했습니다."
            );

            case LectureUpdatedEvent e -> KafkaLectureEventMessage.create(
                    e.getUuid(),
                    LectureEventType.LECTURE_UPDATED,
                    e.getTargetDomainId(),
                    null,        // 전체 공지이므로 특정 학생 ID 없음
                    e.getTutorId(),
                    e.getLectureTitle(),
                    e.getUpdatedDetails() // 상세 변경 내용 전달
            );

            default -> throw new ApplicationException(DomainEventExceptionCode.DOMAIN_EVENT_NOT_SUPPORTED);
        };
    }
}
