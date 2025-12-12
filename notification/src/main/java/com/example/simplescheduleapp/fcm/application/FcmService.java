package com.example.simplescheduleapp.fcm.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.domain.FcmTokenRepository;
import com.example.simplescheduleapp.fcm.domain.service.FcmApiFutureCallback;
import com.example.simplescheduleapp.fcm.domain.service.FcmMessageSender;
import com.example.simplescheduleapp.fcm.exception.FcmTokenExceptionCode;
import com.example.simplescheduleapp.notification.application.event.NotificationRequest;
import com.example.simplescheduleapp.notification.domain.FailedNotification;
import com.example.simplescheduleapp.notification.domain.FailedNotificationRepository;
import com.example.simplescheduleapp.notification.domain.NotificationType;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class FcmService {

    private final FcmTokenRepository fcmTokenRepository;
    private final FailedNotificationRepository failedNotificationRepository;
    private final FcmMessageSender fcmMessageSender;

    public void addFcmToken(Long memberId, String fcmToken) {
        FcmToken token = new FcmToken(memberId, fcmToken);
        fcmTokenRepository.save(token);
    }

    public void sendFcmNotification(NotificationRequest event) {
        FcmToken fcmToken = fcmTokenRepository.getByMemberId(event.targetId());

        if (fcmToken == null || fcmToken.getFcmToken() == null) {
            log.warn("FCM 토큰이 존재하지 않아 전송에 실패했습니다. targetId: {}", event.targetId());
            saveFailedNotification(event, FcmTokenExceptionCode.FCM_TOKEN_NOT_FOUND.getMessage());
            return;
        }

        // 1. 메시지 전송 요청
        ApiFuture<String> future = fcmMessageSender.sendFcmNotificationAsync(
                fcmToken,
                event.title(),
                event.body()
        );

        // 2. 콜백 생성 및 등록
        FcmApiFutureCallback callback = new FcmApiFutureCallback(
                event,
                fcmToken.getFcmToken(),
                failedNotificationRepository
        );

        ApiFutures.addCallback(future, callback, MoreExecutors.directExecutor());
    }

    // 실패한 메시지 알림 전송 재시도 메서드 (Callback 없음, Future 반환)
    public ApiFuture<String> retryFcmNotification(NotificationRequest event) {
        FcmToken fcmToken = fcmTokenRepository.getByMemberId(event.targetId());

        if (fcmToken == null || fcmToken.getFcmToken() == null) {
            // 토큰이 없으면 즉시 예외를 던져서 호출자가 처리하게 함
            throw new ApplicationException(FcmTokenExceptionCode.FCM_TOKEN_NOT_FOUND);
        }

        // 콜백 없이 Future만 반환
        return fcmMessageSender.sendFcmNotificationAsync(
                fcmToken,
                event.title(),
                event.body()
        );
    }

    private void saveFailedNotification(NotificationRequest event, String reason) {
        FailedNotification failedNotification = new FailedNotification(
                event.senderId(),
                event.targetId(),
                event.title(),
                event.body(),
                NotificationType.FCM,
                reason
        );
        failedNotificationRepository.save(failedNotification);
    }
}
