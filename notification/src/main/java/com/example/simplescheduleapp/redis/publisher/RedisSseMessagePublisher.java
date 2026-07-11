package com.example.simplescheduleapp.redis.publisher;

import com.example.simplescheduleapp.common.messaging.MessagePublisher;
import com.example.simplescheduleapp.notification.application.event.NotificationRequest;
import com.example.simplescheduleapp.redis.topic.RedisChannels;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class RedisSseMessagePublisher {

    private final MessagePublisher messagePublisher;
    private final ObjectMapper objectMapper;

    public void publish(NotificationRequest event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            messagePublisher.publish(RedisChannels.SSE_NOTIFICATION, json);
        } catch (JsonProcessingException e) {
            log.error("redis SSE message 발행 실패: {}", e.getMessage());
            throw new RuntimeException("JSON 으로 값을 컨버팅하여 발행하는 도중 문제가 발생하였습니다 --> ", e.getCause());
        }
    }
}
