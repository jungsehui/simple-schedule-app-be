package com.example.simplescheduleapp.sse.event;

import com.example.simplescheduleapp.notification.application.event.NotificationRequest;
import com.example.simplescheduleapp.redis.publisher.RedisSseMessagePublisher;
import com.example.simplescheduleapp.redis.topic.RedisChannels;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RedisSseMessagePublisherTest {

    private StringRedisTemplate stringRedisTemplate;
    private ObjectMapper objectMapper;
    private RedisSseMessagePublisher publisher;

    @BeforeEach
    void setUp() {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        objectMapper = new ObjectMapper();
        publisher = new RedisSseMessagePublisher(stringRedisTemplate, objectMapper);
    }

    @Test
    void publish_메서드가_올바른_JSON을_보내는지_확인() {
        // given
        Long senderMemberId = 1L;
        Long targetMemberId = 123L;
        String title = "테스트 제목입니다";
        String body = "테스트 메시지입니다";
        NotificationRequest event = new NotificationRequest(senderMemberId, targetMemberId, title, body);

        // when
        publisher.publish(event);

        // then
        verify(stringRedisTemplate).convertAndSend(eq(RedisChannels.SSE_NOTIFICATION), argThat((String json) -> {
            try {
                // JSON 문자열을 다시 NotificationRequest 객체로 변환
                NotificationRequest deserializedEvent = objectMapper.readValue(json, NotificationRequest.class);

                // 객체의 각 필드가 기대하는 값과 일치하는지 확인
                assertEquals(senderMemberId, deserializedEvent.senderId());
                assertEquals(targetMemberId, deserializedEvent.targetId());
                assertEquals(title, deserializedEvent.title());
                assertEquals(body, deserializedEvent.body());
                return true; // 모든 검증이 통과하면 true 반환
            } catch (JsonProcessingException e) {
                return false; // 파싱 실패 시 false 반환
            }
        }));
    }
}
