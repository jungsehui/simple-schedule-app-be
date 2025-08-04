package com.example.simplescheduleapp.kafka.topic;

import com.example.simplescheduleapp.notification.domain.NotificationMessage;
import com.example.simplescheduleapp.notification.domain.NotificationMessageType;

public record NotificationDispatchTopicMessage(
        Long notificationId,
        Long senderId,
        Long targetId,
        String message,
        NotificationMessageType type
) {

    public static NotificationDispatchTopicMessage from(NotificationMessage notificationMessage) {
        return new NotificationDispatchTopicMessage(
                notificationMessage.getId(),
                notificationMessage.getSenderMemberId(),
                notificationMessage.getTargetMemberId(),
                notificationMessage.getMessageBody(),
                notificationMessage.getNotificationMessageType()
        );
    }
}
