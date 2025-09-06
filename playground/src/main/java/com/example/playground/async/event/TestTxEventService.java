package com.example.playground.async.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class TestTxEventService {

    private final ApplicationEventPublisher publisher;

    @Transactional
    public void publish(TestDomainEvent event) {
        publisher.publishEvent(event);
    }
}
