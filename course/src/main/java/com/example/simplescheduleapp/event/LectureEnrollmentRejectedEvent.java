package com.example.simplescheduleapp.event;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.event.EventStatus;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import com.example.simplescheduleapp.student.domain.Student;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

/**
 * 수강신청 거절 이벤트 — 순수 도메인 모델 (ADR-0004).
 *
 * <p>아웃박스 영속은 {@code event/infrastructure/persistence}의 대응 엔티티가 담당한다.
 */
@NoArgsConstructor(access = PROTECTED)
@Getter
public class LectureEnrollmentRejectedEvent extends DomainEvent {

    private Long studentId;
    private Long tutorId;
    private String lectureTitle;

    public LectureEnrollmentRejectedEvent(Lecture lecture, Student student) {
        super(lecture.getId()); // targetDomainId = lectureId

        this.studentId = student.getId();
        this.tutorId = lecture.getTutorId();
        this.lectureTitle = lecture.getTitle();
    }

    /** DB 복원용 — 영속 매퍼 전용. */
    public LectureEnrollmentRejectedEvent(Long id, String uuid, EventStatus status, Long targetDomainId,
                                          String failReason, int retryCount,
                                          Long studentId, Long tutorId, String lectureTitle) {
        super(id, uuid, status, targetDomainId, failReason, retryCount);
        this.studentId = studentId;
        this.tutorId = tutorId;
        this.lectureTitle = lectureTitle;
    }

    @Override
    public String getTopic() {
        return KafkaTopics.COURSE_EVENT_TOPIC;
    }
}
