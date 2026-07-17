package com.example.simplescheduleapp.event.infrastructure.persistence;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.outbox.persistence.DomainEventEntity;
import com.example.simplescheduleapp.common.outbox.persistence.DomainEventPersistenceMapper;
import com.example.simplescheduleapp.event.LectureEnrollmentAcceptedEvent;
import org.springframework.stereotype.Component;

/** {@code LectureEnrollmentAcceptedEvent} ↔ 아웃박스 엔티티 변환 (ADR-0004). */
@Component
public class LectureEnrollmentAcceptedEventPersistenceMapper implements DomainEventPersistenceMapper {

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof LectureEnrollmentAcceptedEvent;
    }

    @Override
    public boolean supportsEntity(DomainEventEntity entity) {
        return entity instanceof LectureEnrollmentAcceptedEventEntity;
    }

    @Override
    public DomainEventEntity toEntity(DomainEvent event) {
        LectureEnrollmentAcceptedEvent e = (LectureEnrollmentAcceptedEvent) event;
        return new LectureEnrollmentAcceptedEventEntity(
                e.getId(), e.getUuid(), e.getStatus(), e.getTargetDomainId(),
                e.getFailReason(), e.getRetryCount(),
                e.getStudentId(), e.getTutorId(), e.getLectureTitle());
    }

    @Override
    public DomainEvent toDomain(DomainEventEntity entity) {
        LectureEnrollmentAcceptedEventEntity e = (LectureEnrollmentAcceptedEventEntity) entity;
        return new LectureEnrollmentAcceptedEvent(
                e.getId(), e.getUuid(), e.getStatus(), e.getTargetDomainId(),
                e.getFailReason(), e.getRetryCount(),
                e.getStudentId(), e.getTutorId(), e.getLectureTitle());
    }
}
