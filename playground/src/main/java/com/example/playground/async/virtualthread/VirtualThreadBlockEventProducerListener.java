package com.example.playground.async.virtualthread;

import com.example.playground.async.event.TestDomainEvent;
import com.example.playground.async.event.TestDomainEventRepository;
import com.example.playground.async.event.TestEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.example.playground.async.virtualthread.VirtualThreadAsyncConfig.VIRTUAL_THREAD_ASYNC_TASK_EXECUTOR;

@Slf4j
@RequiredArgsConstructor
@Service
public class VirtualThreadBlockEventProducerListener {

    private final TestDomainEventRepository testDomainEventRepository;
    private final TestEventProducer testEventProducer;

    @Async(VIRTUAL_THREAD_ASYNC_TASK_EXECUTOR)
    @TransactionalEventListener(value = VirtualThreadBlockEvent.class, phase = TransactionPhase.AFTER_COMMIT)
    public void publishEvent(VirtualThreadBlockEvent domainEvent) {
        try {
            log.info("Produce Virtual Thread Block Event. id: {}", domainEvent.getId());
            testEventProducer.produce(domainEvent);
            domainEvent.publishSuccess();
            testDomainEventRepository.save(domainEvent);
            log.info("Successfully produce topic");
        } catch (Exception e) {
            log.error("Exception occurred during producing event. e: {}. message: {}", e.getClass(), e.getMessage());
            domainEvent.publishFail();
            testDomainEventRepository.save(domainEvent);
        }
    }
}
