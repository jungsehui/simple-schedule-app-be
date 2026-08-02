package com.example.simplescheduleapp.event;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.event.EventStatus;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수강신청 취소 이벤트 — 순수 도메인 모델 (ADR-0004).
 *
 * <p>아웃박스 영속은 {@code event/infrastructure/persistence}의 대응 엔티티가 담당한다.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class LectureEnrollmentCanceledEvent extends DomainEvent {

    private Long studentId;      // 취소한 학생 ID
    private Long tutorId;        // 알림 받을 강사 ID
    private String lectureTitle;

    // 취소 시점에는 Student 엔티티 전체보다는 ID만 넘어오는 경우가 많아 ID로 받도록 설계
    public LectureEnrollmentCanceledEvent(Lecture lecture, Long studentId) {
        super(lecture.getId()); // targetDomainId = lectureId

        this.studentId = studentId;
        this.tutorId = lecture.getTutorId();
        this.lectureTitle = lecture.getTitle();
    }

    /** DB 복원용 — 영속 매퍼 전용. */
    public LectureEnrollmentCanceledEvent(Long id, String uuid, EventStatus status, Long targetDomainId,
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
