package com.example.simplescheduleapp.redis.publisher;

import com.example.simplescheduleapp.notification.application.event.NotificationRequest;
import com.example.simplescheduleapp.redis.topic.RedisTopics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class RedisSseMessagePublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(NotificationRequest event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend(RedisTopics.SSE_NOTIFICATION, json);
        } catch (JsonProcessingException e) {
            log.error("redis SSE message 전송 실패: {}", e.getMessage());
            throw new RuntimeException("JSON 으로 값을 컨버팅하여 보내는 도중 문제가 발생하였습니다 --> {}", e.getCause());
        }
    }
}
