package com.example.simplescheduleapp.event.infrastructure.persistence;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.outbox.persistence.DomainEventEntity;
import com.example.simplescheduleapp.common.outbox.persistence.DomainEventPersistenceMapper;
import com.example.simplescheduleapp.event.LectureUpdatedEvent;
import org.springframework.stereotype.Component;

/** {@code LectureUpdatedEvent} ↔ 아웃박스 엔티티 변환 (ADR-0004). */
@Component
public class LectureUpdatedEventPersistenceMapper implements DomainEventPersistenceMapper {

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof LectureUpdatedEvent;
    }

    @Override
    public boolean supportsEntity(DomainEventEntity entity) {
        return entity instanceof LectureUpdatedEventEntity;
    }

    @Override
    public DomainEventEntity toEntity(DomainEvent event) {
        LectureUpdatedEvent e = (LectureUpdatedEvent) event;
        return new LectureUpdatedEventEntity(
                e.getId(), e.getUuid(), e.getStatus(), e.getTargetDomainId(),
                e.getFailReason(), e.getRetryCount(),
                e.getTutorId(), e.getLectureTitle(), e.getUpdatedDetails());
    }

    @Override
    public DomainEvent toDomain(DomainEventEntity entity) {
        LectureUpdatedEventEntity e = (LectureUpdatedEventEntity) entity;
        return new LectureUpdatedEvent(
                e.getId(), e.getUuid(), e.getStatus(), e.getTargetDomainId(),
                e.getFailReason(), e.getRetryCount(),
                e.getTutorId(), e.getLectureTitle(), e.getUpdatedDetails());
    }
}
