package com.example.playground.async.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class TestEventRecorder {

    private final TestDomainEventRepository testDomainEventRepository;

    public TestDomainEvent record(TestDomainEvent event) {
        while (true) {
            try {
                log.info("Try to record event.");
                TestDomainEvent save = testDomainEventRepository.save(event);
                log.info("Successfully record event. id: {}", save.getId());
                return save;
            } catch (DataIntegrityViolationException e) {
                if (e.getMessage().contains("Unique index or primary key violation")) {
                    log.info("Event uuid duplicated. uuid: {}. so record retry", event);
                    event.regenerateUuid();
                } else {
                    log.error("Unexpected exception occurred when record event. e: {}, message: {}",
                            e.getClass(), e.getMessage());
                    throw e;
                }
            }
        }
    }
}
