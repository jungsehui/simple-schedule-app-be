package com.example.simplescheduleapp.notification.domain;

public record NotificationMessageEvent(
        Long senderMemberId,
        Long targetMemberId,
        String title,
        String body
) {
}
