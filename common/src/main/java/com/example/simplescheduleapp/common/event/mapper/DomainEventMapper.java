package com.example.simplescheduleapp.common.event.mapper;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;

public interface DomainEventMapper {

    boolean supports(DomainEvent event);

    KafkaLectureEventMessage mapToMessage(DomainEvent event);
}
