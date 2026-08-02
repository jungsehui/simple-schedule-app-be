package com.example.simplescheduleapp.common.outbox.producer;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.event.producer.EventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.example.simplescheduleapp.common.outbox.producer.EventAsyncTaskExecutorConfig.EVENT_ASYNC_TASK_EXECUTOR;

@Slf4j
@RequiredArgsConstructor
@Service
public class EventProducerListener {

    private final EventProducer eventProducer;

    @Async(EVENT_ASYNC_TASK_EXECUTOR)
    @TransactionalEventListener(value = DomainEvent.class, phase = TransactionPhase.AFTER_COMMIT)
    public void publishEvent(DomainEvent domainEvent) {
        Long eventId = domainEvent.getId();
        String topic = domainEvent.getTopic();
        log.info("Consume cache event. id: {}, topic: {}", eventId, topic);
        eventProducer.produce(domainEvent);
    }
}
