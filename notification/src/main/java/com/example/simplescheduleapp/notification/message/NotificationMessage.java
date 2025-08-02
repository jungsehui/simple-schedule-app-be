package com.example.simplescheduleapp.notification.message;

public record NotificationMessage(
        Long memberId,
        String eventName,
        String messageBody
) {
}
