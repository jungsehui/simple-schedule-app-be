package com.example.simplescheduleapp.common.event.outbox;

import com.example.simplescheduleapp.common.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Service
public class EventRecorderListener {

    private final EventRecorder eventRecorder;

    @TransactionalEventListener(value = DomainEvent.class, phase = TransactionPhase.AFTER_COMMIT)
    public void recordEvent(DomainEvent domainEvent) {
        eventRecorder.record(domainEvent);
    }
}
