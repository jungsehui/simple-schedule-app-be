package com.example.simplescheduleapp.event.infrastructure.persistence;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.outbox.persistence.DomainEventEntity;
import com.example.simplescheduleapp.common.outbox.persistence.DomainEventPersistenceMapper;
import com.example.simplescheduleapp.event.LectureEnrollmentRejectedEvent;
import org.springframework.stereotype.Component;

/** {@code LectureEnrollmentRejectedEvent} ↔ 아웃박스 엔티티 변환 (ADR-0004). */
@Component
public class LectureEnrollmentRejectedEventPersistenceMapper implements DomainEventPersistenceMapper {

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof LectureEnrollmentRejectedEvent;
    }

    @Override
    public boolean supportsEntity(DomainEventEntity entity) {
        return entity instanceof LectureEnrollmentRejectedEventEntity;
    }

    @Override
    public DomainEventEntity toEntity(DomainEvent event) {
        LectureEnrollmentRejectedEvent e = (LectureEnrollmentRejectedEvent) event;
        return new LectureEnrollmentRejectedEventEntity(
                e.getId(), e.getUuid(), e.getStatus(), e.getTargetDomainId(),
                e.getFailReason(), e.getRetryCount(),
                e.getStudentId(), e.getTutorId(), e.getLectureTitle());
    }

    @Override
    public DomainEvent toDomain(DomainEventEntity entity) {
        LectureEnrollmentRejectedEventEntity e = (LectureEnrollmentRejectedEventEntity) entity;
        return new LectureEnrollmentRejectedEvent(
                e.getId(), e.getUuid(), e.getStatus(), e.getTargetDomainId(),
                e.getFailReason(), e.getRetryCount(),
                e.getStudentId(), e.getTutorId(), e.getLectureTitle());
    }
}
