package com.example.simplescheduleapp.kafka.event.mock;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestEventService {

    private final ApplicationEventPublisher eventPublisher;

    public TestEventService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TestDomainEvent publishTestEvent(Long testTargetDomainId) {
        TestDomainEvent event = new TestDomainEvent(testTargetDomainId);
        eventPublisher.publishEvent(event);
        return event;
    }
}
