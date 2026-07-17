package com.example.simplescheduleapp.kafka.event.mock;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.outbox.persistence.DomainEventEntity;
import com.example.simplescheduleapp.common.outbox.persistence.DomainEventPersistenceMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * {@link TestDomainEvent} ↔ {@link TestDomainEventEntity} 변환 (ADR-0004). 테스트 전용.
 *
 * <p>{@link TestDomainEventMapper}와 짝을 이룬다: 그쪽은 Kafka 메시지 변환, 이쪽은 아웃박스 영속 변환.
 */
@Profile("test")
@Component
public class TestDomainEventPersistenceMapper implements DomainEventPersistenceMapper {

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof TestDomainEvent;
    }

    @Override
    public boolean supportsEntity(DomainEventEntity entity) {
        return entity instanceof TestDomainEventEntity;
    }

    @Override
    public DomainEventEntity toEntity(DomainEvent event) {
        TestDomainEvent e = (TestDomainEvent) event;
        return new TestDomainEventEntity(
                e.getId(), e.getUuid(), e.getStatus(), e.getTargetDomainId(),
                e.getFailReason(), e.getRetryCount(), e.rawTopic());
    }

    @Override
    public DomainEvent toDomain(DomainEventEntity entity) {
        TestDomainEventEntity e = (TestDomainEventEntity) entity;
        return new TestDomainEvent(
                e.getId(), e.getUuid(), e.getStatus(), e.getTargetDomainId(),
                e.getFailReason(), e.getRetryCount(), e.getTopic());
    }
}
