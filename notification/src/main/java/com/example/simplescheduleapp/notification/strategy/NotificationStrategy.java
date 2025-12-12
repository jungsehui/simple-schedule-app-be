package com.example.simplescheduleapp.kafka.strategy;

import com.example.simplescheduleapp.common.kafka.topic.CourseEventMessage;
import com.example.simplescheduleapp.common.kafka.topic.CourseEventType;

public interface NotificationStrategy {

    boolean supports(CourseEventType type);

    void handle(CourseEventMessage message);
}
