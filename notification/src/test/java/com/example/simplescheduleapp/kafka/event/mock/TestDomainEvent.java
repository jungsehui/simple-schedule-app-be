package com.example.simplescheduleapp.kafka.event.mock;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.event.EventStatus;

/**
 * 테스트 전용 순수 도메인 이벤트 (ADR-0004).
 *
 * <p>영속은 {@link TestDomainEventEntity}, 변환은 {@link TestDomainEventPersistenceMapper}가 담당한다.
 */
public class TestDomainEvent extends DomainEvent {

    private String topic;

    public TestDomainEvent(String uuid, EventStatus state, Long testTargetDomainId, String topic) {
        super(uuid, state, testTargetDomainId);
        this.topic = topic;
    }

    public TestDomainEvent(Long testTargetDomainId, String topic) {
        super(testTargetDomainId);
        this.topic = topic;
    }

    public TestDomainEvent(Long testTargetDomainId) {
        super(testTargetDomainId);
    }

    public TestDomainEvent() {
    }

    /** DB 복원용 — 영속 매퍼 전용. */
    public TestDomainEvent(Long id, String uuid, EventStatus status, Long targetDomainId,
                           String failReason, int retryCount, String topic) {
        super(id, uuid, status, targetDomainId, failReason, retryCount);
        this.topic = topic;
    }

    @Override
    public String getTopic() {
        return topic == null ? "TEST_DOMAIN_EVENT" : topic;
    }

    /** 매퍼가 원본 값(null 포함)을 그대로 옮기기 위한 접근자 — {@link #getTopic()}은 기본값으로 치환한다. */
    String rawTopic() {
        return topic;
    }
}
