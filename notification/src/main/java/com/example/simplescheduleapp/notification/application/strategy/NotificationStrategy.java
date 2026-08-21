package com.example.simplescheduleapp.notification.application.strategy;

public interface NotificationStrategy {

    NotificationEventType getSupportType();

    void handle(NotificationCommand command);
}
