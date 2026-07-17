package com.example.simplescheduleapp.common.outbox;

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

                // ★ 부여된 식별자를 원본 인스턴스에 전파한다 (ADR-0004).
                // 순수화 이전에는 DomainEvent가 곧 JPA 엔티티라 save()가 원본에 직접 id를 심었고,
                // BEFORE_COMMIT에 기록된 그 인스턴스가 AFTER_COMMIT 발행 리스너로 그대로 넘어갔다.
                // 이제는 매퍼가 만든 다른 인스턴스에 id가 담기므로, 전파하지 않으면 발행 후 save()가
                // UPDATE가 아닌 INSERT가 되어 중복 행이 생기고 원본 행은 INIT으로 남아 릴레이가
                // 무한 재발행한다(at-least-once → 중복 배달). EventRecorderTest가 이를 가드한다.
                domainEvent.assignId(save.getId());

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
