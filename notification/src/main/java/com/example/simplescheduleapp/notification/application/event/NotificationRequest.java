package com.example.simplescheduleapp.notification.application.event;

import com.example.simplescheduleapp.fcm.application.FcmSendRequest;
import com.example.simplescheduleapp.notification.domain.FailedNotification;

public record NotificationRequest(
        Long senderId,
        Long targetId,
        String title,
        String body
) {

    /**
     * fcm 슬라이스가 소유한 입력 계약으로 변환한다. 호출자(notification)가 변환을 담당해야
     * fcm이 notification을 몰라도 되고, 슬라이스 순환이 생기지 않는다. (ADR-0004)
     */
    public FcmSendRequest toFcmSendRequest() {
        return new FcmSendRequest(senderId, targetId, title, body);
    }

    public static NotificationRequest fromFail(FailedNotification failedNotification) {
        return new NotificationRequest(
                failedNotification.getSenderId(),
                failedNotification.getTargetId(),
                failedNotification.getTitle(),
                failedNotification.getBody()
        );
    }
}
