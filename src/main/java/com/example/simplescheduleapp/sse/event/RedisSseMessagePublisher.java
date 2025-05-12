package com.example.simplescheduleapp.sse.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Configuration
public class RedisSseMessagePublisher {

    private final StringRedisTemplate stringRedisTemplate;

    public void publish(Long memberId, String eventName, String message) {
        Map<String, String> payload = new HashMap<>();
        payload.put("memberId", memberId.toString());
        payload.put("eventName", eventName);
        payload.put("message", message);

        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(payload);
            stringRedisTemplate.convertAndSend("sse-notification", json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 으로 값을 컨버팅하여 보내는 도중 문제가 발생하였습니다 !!", e);
        }
    }
}
