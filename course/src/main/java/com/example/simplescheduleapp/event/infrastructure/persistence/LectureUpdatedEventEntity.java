package com.example.simplescheduleapp.event.infrastructure.persistence;

import com.example.simplescheduleapp.common.event.EventStatus;
import com.example.simplescheduleapp.common.outbox.persistence.DomainEventEntity;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** {@code LectureUpdatedEvent}의 아웃박스 영속 모델 (ADR-0004). discriminator·컬럼은 순수화 이전과 동일. */
@DiscriminatorValue("LECTURE_UPDATED")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class LectureUpdatedEventEntity extends DomainEventEntity {

    private Long tutorId;
    private String lectureTitle;
    private String updatedDetails;

    public LectureUpdatedEventEntity(Long id, String uuid, EventStatus status, Long targetDomainId,
                                     String failReason, int retryCount,
                                     Long tutorId, String lectureTitle, String updatedDetails) {
        super(id, uuid, status, targetDomainId, failReason, retryCount);
        this.tutorId = tutorId;
        this.lectureTitle = lectureTitle;
        this.updatedDetails = updatedDetails;
    }
}
