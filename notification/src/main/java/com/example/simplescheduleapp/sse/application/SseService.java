package com.example.simplescheduleapp.sse.application;

import com.example.simplescheduleapp.notification.application.event.NotificationRequest;
import com.example.simplescheduleapp.redis.publisher.RedisSseMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class SseService {

    private final RedisSseMessagePublisher redisSseMessagePublisher;

    public void sendSseNotification(NotificationRequest message) {
        log.info("Redis 를 통해 targetId: {} 에게 이벤트 발행 - title: {}, body: {}",
                message.targetId(), message.title(), message.body());
        redisSseMessagePublisher.publish(message);
    }
}
