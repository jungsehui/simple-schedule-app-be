package com.example.playground.requiresnewdeadlock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class DeadLockTestAfterCommitEventListener {

    private final Logger log = LoggerFactory.getLogger(DeadLockTestAfterCommitEventListener.class);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // AFTER_COMMIT -> 기본값이지만 테스트 확인용 명시
    public void handleEvent(TestEvent event) {
        log.info("call DeadLockTestAfterCommitEventListener.handleEvent(). event-id: {}", event.getId());
    }
}
