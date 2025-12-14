package com.example.simplescheduleapp.kafka.event.mock;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.event.EventStatus;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@DiscriminatorValue("TEST_DOMAIN_EVENT")
@Entity
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

    @Override
    public KafkaLectureEventMessage toMessage() {
        // 테스트 목적에 따라 null을 반환하거나, 필요하다면 더미 객체를 생성해서 반환
        return null;

        /* 만약 실제 객체가 필요하다면:
        return new KafkaLectureEventMessage(
                this.getUuid(),
                LectureEventType.LECTURE_UPDATED, // 임의 타입
                this.getTargetDomainId(),
                1L, 1L, "Test Title", "Test Details"
        );
        */
    }

    public TestDomainEvent() {
    }

    @Override
    public String getTopic() {
        return topic == null ? "TEST_DOMAIN_EVENT" : topic;
    }
}
