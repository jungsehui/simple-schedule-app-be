package com.example.simplescheduleapp.kafka.event.mock;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.event.EventState;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@DiscriminatorValue("TEST_DOMAIN_EVENT")
@Entity
public class TestDomainEvent extends DomainEvent {

    private String topic;

    public TestDomainEvent(String uuid, EventState state, Long testTargetDomainId, String topic) {
        super(uuid, state, testTargetDomainId);
        this.topic = topic;
    }

    public TestDomainEvent(Long testTargetDomainId, String topic) {
        super(testTargetDomainId);
        this.topic = topic;
    }

    public TestDomainEvent(Long testTargetDomainId) {
        super(testTargetDomainId);
    }

    public TestDomainEvent() {
    }

    @Override
    public String getTopic() {
        return topic == null ? "TEST_DOMAIN_EVENT" : topic;
    }
}
