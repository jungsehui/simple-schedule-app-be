package com.example.simplescheduleapp.fcm.infrastructure;

import com.example.simplescheduleapp.notification.application.event.NotificationRequest;
import com.example.simplescheduleapp.notification.domain.FailedNotification;
import com.example.simplescheduleapp.notification.domain.FailedNotificationRepository;
import com.example.simplescheduleapp.notification.domain.NotificationType;
import com.google.api.core.ApiFutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Firebase 비동기 전송 콜백 어댑터.
 * (기존 fcm.domain.service 위치에서 이동 — 도메인이 application의 NotificationRequest를
 * 역참조하던 문제를 infrastructure로 옮겨 해소, ADR-0002 Stage 1)
 */
@Slf4j
@RequiredArgsConstructor
public class FcmApiFutureCallback implements ApiFutureCallback<String> {

    private final NotificationRequest event;
    private final String fcmToken;
    private final FailedNotificationRepository failedNotificationRepository;

    @Override
    public void onFailure(Throwable t) {
        log.error("FCM 비동기 전송 실패 - target member Id: {}, token: {}, exception: {}",
                event.targetId(), fcmToken, t.getMessage());

        FailedNotification failedNotification = new FailedNotification(
                event.senderId(),
                event.targetId(),
                event.title(),
                event.body(),
                NotificationType.FCM,
                t.getMessage()
        );
        failedNotificationRepository.save(failedNotification);
        log.info("실패한 FCM 알림을 DB에 저장했습니다. targetId: {}", event.targetId());
    }

    @Override
    public void onSuccess(String result) {
        log.info("FCM 비동기 전송 성공 - result: {}, target member ID: {}",
                result, event.targetId());
    }
}
