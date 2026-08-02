package com.example.simplescheduleapp.event.infrastructure.persistence;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.outbox.persistence.DomainEventEntity;
import com.example.simplescheduleapp.common.outbox.persistence.DomainEventPersistenceMapper;
import com.example.simplescheduleapp.event.LectureEnrollmentCanceledEvent;
import org.springframework.stereotype.Component;

/** {@code LectureEnrollmentCanceledEvent} ↔ 아웃박스 엔티티 변환 (ADR-0004). */
@Component
public class LectureEnrollmentCanceledEventPersistenceMapper implements DomainEventPersistenceMapper {

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof LectureEnrollmentCanceledEvent;
    }

    @Override
    public boolean supportsEntity(DomainEventEntity entity) {
        return entity instanceof LectureEnrollmentCanceledEventEntity;
    }

    @Override
    public DomainEventEntity toEntity(DomainEvent event) {
        LectureEnrollmentCanceledEvent e = (LectureEnrollmentCanceledEvent) event;
        return new LectureEnrollmentCanceledEventEntity(
                e.getId(), e.getUuid(), e.getStatus(), e.getTargetDomainId(),
                e.getFailReason(), e.getRetryCount(),
                e.getStudentId(), e.getTutorId(), e.getLectureTitle());
    }

    @Override
    public DomainEvent toDomain(DomainEventEntity entity) {
        LectureEnrollmentCanceledEventEntity e = (LectureEnrollmentCanceledEventEntity) entity;
        return new LectureEnrollmentCanceledEvent(
                e.getId(), e.getUuid(), e.getStatus(), e.getTargetDomainId(),
                e.getFailReason(), e.getRetryCount(),
                e.getStudentId(), e.getTutorId(), e.getLectureTitle());
    }
}
