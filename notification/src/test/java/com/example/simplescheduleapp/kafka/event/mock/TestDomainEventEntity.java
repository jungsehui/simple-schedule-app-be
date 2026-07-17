package com.example.simplescheduleapp.kafka.event.mock;

import com.example.simplescheduleapp.common.event.EventStatus;
import com.example.simplescheduleapp.common.outbox.persistence.DomainEventEntity;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** {@link TestDomainEvent}의 아웃박스 영속 모델 (ADR-0004). 테스트 전용. */
@DiscriminatorValue("TEST_DOMAIN_EVENT")
@Entity
public class TestDomainEventEntity extends DomainEventEntity {

    private String topic;

    protected TestDomainEventEntity() {
    }

    public TestDomainEventEntity(Long id, String uuid, EventStatus status, Long targetDomainId,
                                 String failReason, int retryCount, String topic) {
        super(id, uuid, status, targetDomainId, failReason, retryCount);
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }
}
