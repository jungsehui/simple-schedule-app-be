package com.example.simplescheduleapp.kafka.topic;

import com.example.simplescheduleapp.notification.domain.NotificationMessage;

public record NotificationDispatchEvent(
        Long notificationId,
        Long senderMemberId,
        Long targetMemberId,
        String messageBody
) {

    public static NotificationDispatchEvent from(NotificationMessage notificationMessage) {
        return new NotificationDispatchEvent(
                notificationMessage.getId(),
                notificationMessage.getSenderMemberId(),
                notificationMessage.getTargetMemberId(),
                notificationMessage.getMessageBody()
        );
    }
}
