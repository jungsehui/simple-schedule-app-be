package com.example.playground.async.singlethread;

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

import static com.example.playground.async.singlethread.SingleThreadAsyncConfig.SINGLE_THREAD_ASYNC_TASK_EXECUTOR;

@Slf4j
@RequiredArgsConstructor
@Service
public class SingleThreadEventProducerListener {

    private final TestDomainEventRepository testDomainEventRepository;
    private final TestEventProducer testEventProducer;

//    @Async(SINGLE_THREAD_ASYNC_TASK_EXECUTOR)
//    @TransactionalEventListener(value = SingleThreadBlockEvent.class, phase = TransactionPhase.AFTER_COMMIT)
//    public void produceEvent(SingleThreadBlockEvent domainEvent) {
//        try {
//            log.info("Produce Single Thread Block Event. id: {}", domainEvent.getId());
//            testEventProducer.produce(domainEvent);
//            domainEvent.publishSuccess();
//            testDomainEventRepository.save(domainEvent);
//        } catch (Exception e) {
//            log.error("Exception occurred during producing event. e: {}. message: {}", e.getClass(), e.getMessage());
//            domainEvent.publishFail();
//            testDomainEventRepository.save(domainEvent);
//        }
//    }

    // SingleThreadEventProducerListener.java
    @Async(SINGLE_THREAD_ASYNC_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void produceEvent(SingleThreadBlockEvent domainEvent) {
        try {
            // ID를 사용해 DB에서 최신 상태의 Event 객체를 다시 조회한다.
            TestDomainEvent freshEvent = testDomainEventRepository.findById(domainEvent.getId())
                    .orElseThrow(() -> new IllegalStateException("Event not found with id: " + domainEvent.getId()));

            testEventProducer.produce(freshEvent);
            freshEvent.publishSuccess(); // 이제 getCreatedDate()는 DB에서 가져온 값이므로 null이 아님
            testDomainEventRepository.save(freshEvent);
        } catch (Exception e) {
            log.error("Exception occurred during producing event. e: {}. message: {}", e.getClass(), e.getMessage());

            // 실패 시에도 DB에서 다시 조회해서 상태를 업데이트해야 함
            TestDomainEvent eventToFail = testDomainEventRepository.findById(domainEvent.getId())
                    .orElseThrow(() -> new IllegalStateException("Event not found with id: " + domainEvent.getId()));
            eventToFail.publishFail();
            testDomainEventRepository.save(eventToFail);
        }
    }
}
