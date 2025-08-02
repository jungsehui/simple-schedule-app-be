package com.example.simplescheduleapp.sse.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RedisSseMessagePublisherTest {

    private StringRedisTemplate stringRedisTemplate;
    private RedisSseMessagePublisher publisher;

    @BeforeEach
    void setUp() {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        publisher = new RedisSseMessagePublisher(stringRedisTemplate);
    }

    @Test
    void publish_메서드가_올바른_JSON을_보내는지_확인() {
        // given
        Long memberId = 1L;
        String eventName = "test-event";
        String message = "테스트 메시지입니다";

        // when
        publisher.publish(memberId, eventName, message);

        // then
        verify(stringRedisTemplate).convertAndSend(eq("sse-notification"), argThat((String json)
                -> json.contains("\"memberId\":\"1\"")
                && json.contains("\"eventName\":\"test-event\"")
                && json.contains("\"message\":\"테스트 메시지입니다\"")));

    }
}
