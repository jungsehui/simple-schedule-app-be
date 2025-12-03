package com.example.playground.requiresnewdeadlock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeadLockTestEventProducer {

    private final Logger log = LoggerFactory.getLogger(DeadLockTestEventProducer.class);

    private final ApplicationEventPublisher publisher;

    public DeadLockTestEventProducer(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Transactional
    public void publishEvent(TestEvent event) {
        // 코드 작성
        log.info("call DeadLockTestEventProducer.publish(). event-id: {}", event.getId());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("wake up! event-id: {}", event.getId());
        publisher.publishEvent(event);
        log.info("call publisher.publishEvent()! event-id: {}", event.getId());
    }
}
