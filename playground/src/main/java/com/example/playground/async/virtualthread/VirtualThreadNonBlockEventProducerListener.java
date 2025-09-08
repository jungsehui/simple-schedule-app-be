package com.example.playground.async.virtualthread;

import com.example.playground.async.event.TestDomainEventPublishCallback;
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
public class VirtualThreadNonBlockEventProducerListener {

    private final TestDomainEventPublishCallback callback;
    private final TestDomainEventRepository testDomainEventRepository;
    private final TestEventProducer testEventProducer;

    @Async(VIRTUAL_THREAD_ASYNC_TASK_EXECUTOR)
    @TransactionalEventListener(value = VirtualThreadNonBlockEvent.class, phase = TransactionPhase.AFTER_COMMIT)
    public void publishEvent(VirtualThreadNonBlockEvent domainEvent) {
        try {
            log.info("Produce Virtual Thread Non Block Event. id: {}", domainEvent.getId());
            testEventProducer.produce(domainEvent, callback);
        } catch (Exception e) {
            log.error("Exception occurred during producing event. e: {}. message: {}", e.getClass(), e.getMessage());
            domainEvent.publishFail();
            testDomainEventRepository.save(domainEvent);
        }
    }
}
