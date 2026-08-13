package com.example.simplescheduleapp.notification.application;

import com.example.simplescheduleapp.fcm.application.FcmService;
import com.example.simplescheduleapp.notification.application.event.NotificationRequest;
import com.example.simplescheduleapp.sse.application.SseConnectionPublisher;
import com.example.simplescheduleapp.sse.application.port.out.SseClientPresence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationDispatcher {

    private final SseConnectionPublisher sseConnectionPublisher;
    private final FcmService fcmService;
    private final SseClientPresence sseClientPresence;

    public void dispatchPushNotification(NotificationRequest request) {
        // SSE가 연결되어 있으면 SSE로 보내고, 그렇지 않으면 FCM으로 보낸다.
        if (sseClientPresence.isClientConnected(request.targetId())) {
            try {
                sseConnectionPublisher.publishSseNotification(request);
            } catch (Exception e) {
                log.warn("SSE 전송 실패. FCM으로 대체 전송합니다. targetId: {}", request.targetId(), e);
                fcmService.sendFcmNotification(request.toFcmSendRequest());
            }
        } else {
            // SSE가 연결되어 있지 않다면 바로 FCM 전송
            fcmService.sendFcmNotification(request.toFcmSendRequest());
        }
    }
}
