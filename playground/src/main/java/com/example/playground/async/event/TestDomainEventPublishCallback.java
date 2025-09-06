package com.example.playground.async.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.function.BiConsumer;

@Slf4j
@RequiredArgsConstructor
@Service
public class TestDomainEventPublishCallback implements BiConsumer<SendResult<String, Object>, Throwable> {

    private final TestDomainEventRepository testDomainEventRepository;

    @Override
    public void accept(SendResult<String, Object> result, Throwable throwable) {
        String value = (String) result.getProducerRecord().value();
        if (throwable == null) {
            log.info("(callback) Successfully produced topic. eventId: {}", value);
            TestDomainEvent event = testDomainEventRepository.getByUuid(value);
            event.publishSuccess();
            testDomainEventRepository.save(event);
            return;
        }
        log.error("(callback) Unexpected exception while produce topic to broker. "
                        + "eventId: {}, e: {}, message: {}",
                value, throwable.getClass(), throwable.getMessage());
        TestDomainEvent event = testDomainEventRepository.getByUuid(value);
        event.publishFail();
        testDomainEventRepository.save(event);
        log.info("Successfully failed the topic. eventId: {}", value);
    }
}
