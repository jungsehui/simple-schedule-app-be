package com.example.simplescheduleapp.notification.application;

import com.example.simplescheduleapp.fcm.application.FcmService;
import com.example.simplescheduleapp.notification.message.NotificationMessage;
import com.example.simplescheduleapp.sse.application.SseService;
import com.example.simplescheduleapp.sse.domain.RedisClientManager;
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

    public void sendPushNotification(NotificationMessage message) {
        if (redisClientManager.isClientConnected(message.memberId())) {
            sseService.sendNotification(message);
        } else {
            fcmService.sendPushNotification(message);
        }
    }
}
