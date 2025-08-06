package com.example.simplescheduleapp.common.event.producer;

import com.example.simplescheduleapp.common.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Service
public class EventProducerListener {

    private final EventProducer eventProducer;

    @TransactionalEventListener(value = DomainEvent.class, phase = TransactionPhase.AFTER_COMMIT)
    public void publishEvent(DomainEvent domainEvent) {
        Long eventId = domainEvent.getId();
        String topic = domainEvent.getTopic();
        log.info("Consume domain event. id: {}, topic: {}", eventId, topic);
        eventProducer.produce(domainEvent);
    }
}
