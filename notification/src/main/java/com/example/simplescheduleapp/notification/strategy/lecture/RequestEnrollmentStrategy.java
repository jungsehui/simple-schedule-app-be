package com.example.simplescheduleapp.kafka.strategy.lecture;

import com.example.simplescheduleapp.common.kafka.topic.CourseEventMessage;
import com.example.simplescheduleapp.common.kafka.topic.CourseEventType;
import com.example.simplescheduleapp.kafka.strategy.NotificationStrategy;
import com.example.simplescheduleapp.notification.application.NotificationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class RequestEnrollmentStrategy implements NotificationStrategy {

    private final NotificationFacade notificationFacade;

    @Override
    public boolean supports(CourseEventType type) {
        return type == CourseEventType.REQUEST_ENROLLMENT;
    }

    @Override
    public void handle(CourseEventMessage message) {
        notificationFacade.sendNotification(
                message.senderId(), message.targetId(), message.title(), message.content()
        );
    }
}
