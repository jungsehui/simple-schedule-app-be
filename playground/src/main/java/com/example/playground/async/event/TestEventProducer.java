package com.example.playground.async.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class TestEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void produce(TestDomainEvent event) {
        log.info("Try to produce event: {}, id: {}", event.getClass().getSimpleName(), event.getId());
        try {
            // get()으로 응답 완료까지 block
            kafkaTemplate.send(event.getClass().getSimpleName(), event.getUuid()).get();
        } catch (Exception e) {
            log.error("Exception occur!. id: {}", event.getId());
            throw new RuntimeException(e);
        }
    }

    public void publish(TestDomainEvent domainEvent, TestDomainEventPublishCallback callback) {
        log.info("Try to publish event: {}, id: {}", domainEvent.getClass().getSimpleName(), domainEvent.getId());
        try {
            // non block
            kafkaTemplate.send(domainEvent.getClass().getSimpleName(), domainEvent.getUuid()).whenComplete(callback);
        } catch (Exception e) {
            log.error("Exception occur!. id: {}", domainEvent.getId());
            throw new RuntimeException(e);
        }
    }
}
