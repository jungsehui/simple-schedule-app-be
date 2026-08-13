package com.example.simplescheduleapp.notification.application.strategy;

public interface NotificationStrategy {

    default boolean supports(NotificationEventType type) {
        return getSupportType() == type;
    }

    NotificationEventType getSupportType();

    void handle(NotificationCommand command);
}
