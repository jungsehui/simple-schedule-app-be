package com.example.simplescheduleapp.common.event.outbox;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.event.DomainEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class EventRecorder {

    private final DomainEventRepository domainEventRepository;

    private static final int MAX_UUID_RETRY = 3;

    public DomainEvent record(DomainEvent domainEvent) {
        for (int attempt = 0; attempt < MAX_UUID_RETRY; attempt++) {
            try {
                log.info("Try to record event.");
                DomainEvent save = domainEventRepository.save(domainEvent);
                log.info("Successfully record event. id: {}", save.getId());
                return save;
            } catch (DataIntegrityViolationException e) {
                String message = e.getMessage() != null ? e.getMessage() : "";
                if (message.contains("Unique index or primary key violation") || message.contains("Duplicate entry")) {
                    log.info("Event uuid duplicated. uuid: {}. retry attempt: {}", domainEvent, attempt + 1);
                    domainEvent.regenerateUuid();
                } else {
                    log.error("Unexpected exception occurred when record event. e: {}, message: {}",
                            e.getClass(), e.getMessage());
                    throw e;
                }
            }
        }
        throw new DataIntegrityViolationException("Failed to record event after %d UUID regeneration attempts".formatted(MAX_UUID_RETRY));
    }
}
