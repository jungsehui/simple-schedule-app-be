package com.example.simplescheduleapp.event;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.event.EventStatus;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 강의 수정 이벤트 — 순수 도메인 모델 (ADR-0004).
 *
 * <p>아웃박스 영속은 {@code event/infrastructure/persistence}의 {@code LectureUpdatedEventEntity}가,
 * 변환은 같은 패키지의 영속 매퍼가 담당한다.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class LectureUpdatedEvent extends DomainEvent {

    private Long tutorId;
    private String lectureTitle;
    private String updatedDetails; // 수정된 내용 요약

    public LectureUpdatedEvent(Lecture lecture, String updatedDetails) {
        // Target: 여기서는 Lecture ID를 타겟으로 잡음 (1:N 전파를 위해)
        // 컨슈머가 이 ID를 보고 수강생 목록을 조회해야 함
        super(lecture.getId());

        this.tutorId = lecture.getTutorId();
        this.lectureTitle = lecture.getTitle();
        this.updatedDetails = updatedDetails;
    }

    /** DB 복원용 — 영속 매퍼 전용. */
    public LectureUpdatedEvent(Long id, String uuid, EventStatus status, Long targetDomainId,
                               String failReason, int retryCount,
                               Long tutorId, String lectureTitle, String updatedDetails) {
        super(id, uuid, status, targetDomainId, failReason, retryCount);
        this.tutorId = tutorId;
        this.lectureTitle = lectureTitle;
        this.updatedDetails = updatedDetails;
    }

    @Override
    public String getTopic() {
        return KafkaTopics.COURSE_EVENT_TOPIC;
    }
}
