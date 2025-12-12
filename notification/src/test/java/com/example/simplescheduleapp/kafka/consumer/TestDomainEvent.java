package com.example.simplescheduleapp.kafka.consumer;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;

public class TestDomainEvent extends DomainEvent {

    private TestDomainEvent() {
    }

    public TestDomainEvent(Long targetDomainId) {
        super(targetDomainId);
    }

    @Override
    public String getTopic() {
        return KafkaTopics.LECTURE_EVENT_TOPIC; // 테스트 대상 토픽 반환
    }
}
