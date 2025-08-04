package com.example.simplescheduleapp.kafka.event;

public record NotificationMessageEvent(
        Long senderMemberId,
        Long targetMemberId,
        String title,
        String body
) {
}
