package com.example.simplescheduleapp.notification.strategy;

import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.LectureEventType;

public interface NotificationStrategy {

    default boolean supports(LectureEventType type) {
        return getSupportType() == type;
    }

    LectureEventType getSupportType();

    void handle(KafkaLectureEventMessage message);
}
