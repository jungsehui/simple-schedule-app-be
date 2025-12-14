package com.example.simplescheduleapp.kafka.consumer;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;

public class TestDomainEvent extends DomainEvent {

    private TestDomainEvent() {
    }

    public TestDomainEvent(Long targetDomainId) {
        super(targetDomainId);
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

    @Override
    public String getTopic() {
        return KafkaTopics.LECTURE_EVENT_TOPIC; // 테스트 대상 토픽 반환
    }
}
