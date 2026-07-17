package com.example.simplescheduleapp.event.infrastructure.persistence;

import com.example.simplescheduleapp.common.event.EventStatus;
import com.example.simplescheduleapp.common.outbox.persistence.DomainEventEntity;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

/** {@code LectureEnrollmentAcceptedEvent}의 아웃박스 영속 모델 (ADR-0004). */
@DiscriminatorValue("ENROLLMENT_ACCEPTED")
@NoArgsConstructor(access = PROTECTED)
@Getter
@Entity
public class LectureEnrollmentAcceptedEventEntity extends DomainEventEntity {

    private Long studentId;
    private Long tutorId;
    private String lectureTitle;

    public LectureEnrollmentAcceptedEventEntity(Long id, String uuid, EventStatus status, Long targetDomainId,
                                                String failReason, int retryCount,
                                                Long studentId, Long tutorId, String lectureTitle) {
        super(id, uuid, status, targetDomainId, failReason, retryCount);
        this.studentId = studentId;
        this.tutorId = tutorId;
        this.lectureTitle = lectureTitle;
    }
}
