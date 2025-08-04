package com.example.simplescheduleapp.sse.event;

import com.example.simplescheduleapp.notification.domain.NotificationMessageEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

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
        publisher = new RedisSseMessagePublisher(stringRedisTemplate, objectMapper);
    }

    @Test
    void publish_메서드가_올바른_JSON을_보내는지_확인() {
        // given
        Long senderMemberId = 1L;
        Long targetMemberId = 123L;
        String messageBody = "테스트 메시지입니다";

        // when
        publisher.publish(
                new NotificationMessageEvent(
                        senderMemberId,
                        targetMemberId,
                        messageBody
                )
        );

        // then
        verify(stringRedisTemplate).convertAndSend(eq("sse-notification"), argThat((String json)
                -> json.contains("\"senderMemberId\":\"1\"")
                && json.contains("\"targetMemberId\":\"123\"")
                && json.contains("\"messageBody\":\"테스트 메시지입니다\"")));
    }
}
