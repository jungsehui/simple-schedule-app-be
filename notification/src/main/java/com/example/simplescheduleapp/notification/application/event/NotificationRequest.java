package com.example.simplescheduleapp.kafka.event;

import com.example.simplescheduleapp.notification.domain.FailedNotification;

public record NotificationRequest(
        Long senderId,
        Long targetId,
        String title,
        String body
) {

    public static NotificationRequest fromFail(FailedNotification failedNotification) {
        return new NotificationRequest(
                failedNotification.getSenderId(),
                failedNotification.getTargetId(),
                failedNotification.getTitle(),
                failedNotification.getBody()
        );
    }
}
