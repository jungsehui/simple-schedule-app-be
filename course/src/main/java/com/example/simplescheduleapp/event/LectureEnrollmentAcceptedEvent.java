package com.example.simplescheduleapp.event;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.event.EventStatus;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import com.example.simplescheduleapp.student.domain.Student;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수강신청 수락 이벤트 — 순수 도메인 모델 (ADR-0004).
 *
 * <p>아웃박스 영속은 {@code event/infrastructure/persistence}의 대응 엔티티가 담당한다.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class LectureEnrollmentAcceptedEvent extends DomainEvent {

    private Long studentId;
    private Long tutorId;
    private String lectureTitle;

    public LectureEnrollmentAcceptedEvent(Lecture lecture, Student student) {
        // targetDomainId에는 '이 사건의 핵심'인 LectureId를 넣거나,
        // 혹은 Outbox 패턴 식별용으로만 쓰고 비즈니스 로직에선 무시
        super(lecture.getId());

        // 이벤트 당사자들을 모두 명시
        this.studentId = student.getId();
        this.tutorId = lecture.getTutorId(); // 단순 사실 관계 기록용
        this.lectureTitle = lecture.getTitle();
    }

    /** DB 복원용 — 영속 매퍼 전용. */
    public LectureEnrollmentAcceptedEvent(Long id, String uuid, EventStatus status, Long targetDomainId,
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
