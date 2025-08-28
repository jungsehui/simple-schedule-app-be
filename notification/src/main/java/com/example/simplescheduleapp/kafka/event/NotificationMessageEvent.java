package com.example.simplescheduleapp.notification.kafka.event;

public record NotificationMessageEvent(
        Long senderMemberId,
        Long targetMemberId,
        String title,
        String body
) {
}
