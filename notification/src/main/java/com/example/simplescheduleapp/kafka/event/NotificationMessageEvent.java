package com.example.simplescheduleapp.kafka.event;

import com.example.simplescheduleapp.notification.domain.FailedNotification;

public record NotificationMessageEvent(
        Long senderMemberId,
        Long targetMemberId,
        String title,
        String body
) {

    public static NotificationMessageEvent from(FailedNotification failedNotification) {
        return new NotificationMessageEvent(
                failedNotification.getSenderMemberId(),
                failedNotification.getTargetMemberId(),
                failedNotification.getTitle(),
                failedNotification.getBody()
        );
    }
}
