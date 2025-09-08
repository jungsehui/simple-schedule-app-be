package com.example.simplescheduleapp.fcm.domain.service;

import com.example.simplescheduleapp.kafka.event.NotificationMessageEvent;
import com.example.simplescheduleapp.notification.domain.FailedNotification;
import com.example.simplescheduleapp.notification.domain.FailedNotificationRepository;
import com.example.simplescheduleapp.notification.domain.NotificationType;
import com.google.api.core.ApiFutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class FcmApiFutureCallback implements ApiFutureCallback<String> {

    private final NotificationMessageEvent event;
    private final String fcmToken;
    private final FailedNotificationRepository failedNotificationRepository;

    @Override
    public void onFailure(Throwable t) {
        log.error("FCM 비동기 전송 실패 - target member Id: {}, token: {}, exception: {}",
                event.targetMemberId(), fcmToken, t.getMessage());

        FailedNotification failedNotification = new FailedNotification(
                event.senderMemberId(),
                event.targetMemberId(),
                event.title(),
                event.body(),
                NotificationType.FCM,
                t.getMessage()
        );
        failedNotificationRepository.save(failedNotification);
        log.info("실패한 FCM 알림을 DB에 저장했습니다. targetMemberId: {}", event.targetMemberId());
    }

    @Override
    public void onSuccess(String result) {
        log.info("FCM 비동기 전송 성공 - result: {}, target member ID: {}",
                result, event.targetMemberId());
    }
}
