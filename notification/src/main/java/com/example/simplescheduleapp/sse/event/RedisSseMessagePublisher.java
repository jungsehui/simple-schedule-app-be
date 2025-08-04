package com.example.simplescheduleapp.sse.event;

import com.example.simplescheduleapp.kafka.event.NotificationMessageEvent;
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

    public void publish(NotificationMessageEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend("sse-notification", json);
        } catch (JsonProcessingException e) {
            log.error("redis SSE domain 전송 실패: {}", e.getMessage());
            throw new RuntimeException("JSON 으로 값을 컨버팅하여 보내는 도중 문제가 발생하였습니다 --> {}", e.getCause());
        }
    }
}
