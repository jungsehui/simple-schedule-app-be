package com.example.playground.async.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Service
public class TestEventRecordListener {

    private final TestEventRecorder testEventRecorder;

    @TransactionalEventListener(value = TestDomainEvent.class, phase = TransactionPhase.BEFORE_COMMIT)
    public void recordEvent(TestDomainEvent event) {
        testEventRecorder.record(event);
    }
}
