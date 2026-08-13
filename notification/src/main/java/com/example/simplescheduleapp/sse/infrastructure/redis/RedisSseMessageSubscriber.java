package com.example.simplescheduleapp.sse.infrastructure.redis;

import com.example.simplescheduleapp.notification.application.event.NotificationRequest;
import com.example.simplescheduleapp.sse.application.SseConnectionService;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
@Component
public class RedisSseMessageSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SseConnectionService sseConnectionService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            NotificationRequest event = objectMapper.readValue(body, NotificationRequest.class);

            sseConnectionService.sendSseNotification(
                    event.targetId(),
                    event.title(),
                    event.body()
            );
        } catch (Exception e) {
            log.error("Redis 메시지 처리 실패: {}", e.getMessage());
        }
    }
}
