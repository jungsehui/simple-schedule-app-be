package com.example.simplescheduleapp.fcm.application;

import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.domain.FcmTokenRepository;
import com.example.simplescheduleapp.fcm.exception.FcmTokenExceptionCode;
import com.example.simplescheduleapp.fcm.utils.FcmUtils;
import com.example.simplescheduleapp.kafka.event.NotificationMessageEvent;
import com.example.simplescheduleapp.notification.domain.FailedNotification;
import com.example.simplescheduleapp.notification.domain.FailedNotificationRepository;
import com.example.simplescheduleapp.notification.domain.NotificationType;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class FcmService {

    private final FcmTokenRepository fcmTokenRepository;
    private final FailedNotificationRepository failedNotificationRepository;

    public void addFcmToken(Long memberId, String fcmToken) {
        FcmToken token = new FcmToken(memberId, fcmToken);
        fcmTokenRepository.save(token);
    }

    public void sendFcmNotification(NotificationMessageEvent event) {
        FcmToken fcmToken = fcmTokenRepository.getByMemberId(event.targetMemberId());

        if (fcmToken == null || fcmToken.getFcmToken() == null) {
            log.warn("FCM 토큰이 존재하지 않아 전송에 실패했습니다. targetMemberId: {}", event.targetMemberId());
            saveFailedNotification(event, FcmTokenExceptionCode.FCM_TOKEN_NOT_FOUND.getMessage());
            return;
        }

        Notification notification = FcmUtils.createNotification(event.title(), event.body());
        Message toSend = FcmUtils.buildMessage(fcmToken.getFcmToken(), notification);

        ApiFuture<String> future = FirebaseMessaging.getInstance().sendAsync(toSend);

        ApiFutures.addCallback(future, new ApiFutureCallback<>() {
            // 전송 성공 시 호출될 콜백
            @Override
            public void onSuccess(String response) {
                log.info("FCM 비동기 전송 성공 - response: {}, target member ID: {}",
                        response, event.targetMemberId());
            }

            // 전송 실패 시 호출될 콜백
            @Override
            public void onFailure(Throwable t) {
                log.error("FCM 비동기 전송 실패 - target member Id: {}, token: {}, exception: {}",
                        event.targetMemberId(), fcmToken.getFcmToken(), t.getMessage());
                // 실패한 알림을 DB에 저장하여 재처리 대상으로 만듦
                log.info("Failed Notification 저장 시도 - target member Id: {}, token: {}, exception: {}",
                        event.targetMemberId(), fcmToken.getFcmToken(), t.getMessage());
                saveFailedNotification(event, t.getMessage());
                log.info("Failed Notification 저장 완료 - target member Id: {}, token: {}, exception: {}",
                        event.targetMemberId(), fcmToken.getFcmToken(), t.getMessage());
            }
        }, MoreExecutors.directExecutor());
    }

    private void saveFailedNotification(NotificationMessageEvent event, String reason) {
        FailedNotification failedNotification = new FailedNotification(
                event.senderMemberId(),
                event.targetMemberId(),
                event.title(),
                event.body(),
                NotificationType.FCM,
                reason
        );
        failedNotificationRepository.save(failedNotification);
    }
}
