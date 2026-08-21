package com.example.simplescheduleapp.sse.application;

import com.example.simplescheduleapp.notification.application.event.NotificationRequest;
import com.example.simplescheduleapp.sse.application.port.out.SseMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class SseConnectionPublisher {

    private final SseMessagePublisher sseMessagePublisher;

    public void publishSseNotification(NotificationRequest message) {
        log.info("Redis 를 통해 targetId: {} 에게 이벤트 발행 - title: {}, body: {}",
                message.targetId(), message.title(), message.body());
        sseMessagePublisher.publish(message);
    }
}
