package com.example.simplescheduleapp.notification.application;

import com.example.simplescheduleapp.fcm.application.FcmService;
import com.example.simplescheduleapp.notification.application.event.NotificationRequest;
import com.example.simplescheduleapp.sse.application.SseService;
import com.example.simplescheduleapp.redis.cache.RedisClientManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationService {

    private final SseService sseService;
    private final FcmService fcmService;
    private final RedisClientManager redisClientManager;

    public void sendPushNotification(NotificationRequest event) {
        // SSE가 연결되어 있으면 SSE로 보내고, 그렇지 않으면 FCM으로 보낸다.
        if (redisClientManager.isClientConnected(event.targetId())) {
            try {
                sseService.sendSseNotification(event);
            } catch (Exception e) {
                log.warn("SSE 전송 실패. FCM으로 대체 전송합니다. targetId: {}", event.targetId());
                fcmService.sendFcmNotification(event);
            }
        } else {
            // SSE가 연결되어 있지 않다면 바로 FCM 전송
            fcmService.sendFcmNotification(event);
        }
    }
}
