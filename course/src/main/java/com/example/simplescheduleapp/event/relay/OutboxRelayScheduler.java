package com.example.simplescheduleapp.event.relay;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.event.DomainEventRepository;
import com.example.simplescheduleapp.common.event.EventStatus;
import com.example.simplescheduleapp.common.event.producer.EventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class OutboxRelayScheduler {

    private static final int MAX_RETRY_COUNT = 5;
    private static final int STALE_THRESHOLD_SECONDS = 30;

    private final DomainEventRepository domainEventRepository;
    private final EventProducer eventProducer;

    @Scheduled(fixedDelay = 5000)
    public void relayOutboxEvents() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(STALE_THRESHOLD_SECONDS);
        List<DomainEvent> staleEvents = domainEventRepository
                .findByStatusAndCreatedDateBefore(EventStatus.INIT, threshold);

        List<DomainEvent> failedEvents = domainEventRepository
                .findByStatus(EventStatus.PRODUCE_FAIL);

        List<DomainEvent> allEvents = new ArrayList<>();
        allEvents.addAll(staleEvents);
        allEvents.addAll(failedEvents);

        if (!allEvents.isEmpty()) {
            log.info("Outbox relay: {} events to process (stale={}, failed={})",
                    allEvents.size(), staleEvents.size(), failedEvents.size());
        }

        for (DomainEvent event : allEvents) {
            relaySingle(event);
        }
    }

    private void relaySingle(DomainEvent event) {
        if (event.getRetryCount() >= MAX_RETRY_COUNT) {
            event.markDead();
            domainEventRepository.save(event);
            log.error("Outbox event exceeded max retries. Marked as DEAD. id={}, uuid={}",
                    event.getId(), event.getUuid());
            return;
        }

        try {
            event.incrementRetryCount();
            eventProducer.produce(event);
        } catch (Exception e) {
            log.warn("Outbox relay failed. id={}, retryCount={}",
                    event.getId(), event.getRetryCount(), e);
        }
    }
}
