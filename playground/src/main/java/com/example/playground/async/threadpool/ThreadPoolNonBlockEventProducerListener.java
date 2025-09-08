package com.example.playground.async.threadpool;

import com.example.playground.async.event.TestDomainEventRepository;
import com.example.playground.async.event.TestEventProducer;
import com.example.playground.async.singlethread.SingleThreadBlockEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.example.playground.async.singlethread.SingleThreadAsyncConfig.SINGLE_THREAD_ASYNC_TASK_EXECUTOR;
import static com.example.playground.async.threadpool.ThreadPoolAsyncConfig.THREAD_POOL_ASYNC_TASK_EXECUTOR;

@Slf4j
@RequiredArgsConstructor
@Service
public class ThreadPoolBlockEventProducerListener {

    private final TestDomainEventRepository testDomainEventRepository;
    private final TestEventProducer testEventProducer;

    @Async(THREAD_POOL_ASYNC_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void produceEvent(SingleThreadBlockEvent domainEvent) {
        try {
            testEventProducer.produce(domainEvent);
            domainEvent.publishSuccess();
            testDomainEventRepository.save(domainEvent);
        } catch (Exception e) {
            log.error("Exception occurred during producing event. e: {}. message: {}", e.getClass(), e.getMessage());
            domainEvent.publishFail();
            testDomainEventRepository.save(domainEvent);
        }
    }
}
